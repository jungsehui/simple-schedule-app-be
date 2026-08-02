package com.example.simplescheduleapp.lecture.special.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.special.application.command.SpecialLectureEnrollmentCreateCommand;
import com.example.simplescheduleapp.lecture.special.config.DefenseProperties;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureEnrollment;
import com.example.simplescheduleapp.lecture.special.exception.SpecialLectureEnrollmentExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * 특강 수강 신청을 4단계 방어 구조로 처리하는 오케스트레이터.
 *
 * <h2>요청 흐름</h2>
 * <pre>
 *   요청
 *    │
 *    ├─[1차] Redis Atomic DECR  (캐시 계층 사전 필터링)
 *    │       정원 초과면 즉시 거절. DB까지 도달하는 요청을 정원 수준으로 줄인다.
 *    │
 *    ├─[2차] Redisson FairLock  (Critical Section 보호, Feature Toggle)
 *    │       1차를 통과한 요청들이 DB INSERT 시 race condition을 일으키지 않도록 직렬화.
 *    │       {@link LockedSpecialLectureEnroller}가 외부 빈으로서 AOP를 적용한다.
 *    │
 *    ├─[3차] DB 낙관적 락(@Version) (데이터베이스 계층 최종 정합성)
 *    │       2차가 정상 동작하면 충돌이 없으므로 비용 ≈ 0.
 *    │       2차가 실패(leaseTime 만료/네트워크 파티션)했을 때만 발동한다.
 *    │
 *    └─[4차] UK 제약 (DB 레벨 중복 방지)
 *           동일 학생의 중복 INSERT를 DB가 물리적으로 거부.
 * </pre>
 *
 * <h2>트랜잭션 경계</h2>
 * 본 메서드는 외부 {@code @Transactional}을 두지 <b>않는다</b>. 이유:
 * <ul>
 *   <li>Redis 작업은 DB 트랜잭션과 무관하다 → 명시적 보상으로 정합성을 보장한다.</li>
 *   <li>2차 분산 락 AOP가 {@code REQUIRES_NEW} 전파로 별도 트랜잭션을 시작-커밋한 뒤 락을 푼다.
 *       락 해제와 트랜잭션 커밋의 순서가 정렬되어 있어, 락 풀린 시점에 DB는 이미 일관된 상태다.</li>
 *   <li>외부에 또 다른 트랜잭션이 있으면 nested 트랜잭션이 되어 격리 수준 추론이 어려워진다.</li>
 * </ul>
 *
 * <h2>보상(compensation) 정책</h2>
 * 1차 Redis DECR 후 2~4차 단계에서 어떤 예외가 발생하든 Redis INCR로 정원을 복구한다.
 * 단, 프로세스가 비정상 종료되면 보상 코드가 실행되지 않으므로,
 * 별도의 정합성 스케줄러로 Redis ↔ DB의 차이를 주기적으로 보정해야 한다(향후 개선).
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RedisSpecialLectureEnrollmentService {

    private final SpecialLectureRedisClient specialLectureRedisClient;
    private final SpecialLectureEnrollmentService specialLectureEnrollmentService;
    private final LockedSpecialLectureEnroller lockedSpecialLectureEnroller;
    private final DefenseProperties defenseProperties;

    public void enrollSpecialLectureEnrollment(SpecialLectureEnrollmentCreateCommand command) {
        // 1차 방어선: Redis Atomic DECR
        specialLectureRedisClient.enrollSpecialLectureEnrollment(command.specialLectureId());

        try {
            SpecialLectureEnrollment enrollment = saveEnrollment(command);
            log.info("특강 신청이 완료되었습니다. 학생 ID {} 특강 ID {}.",
                    enrollment.getStudentId(),
                    enrollment.getSpecialLectureId());
        } catch (Exception e) {
            // 보상: 어떤 실패든 Redis 카운터 복구
            log.error("DB 저장 실패 .. Redis 보상 트랜잭션 수행. cause: {} message: {}",
                    e.getClass().getSimpleName(), e.getMessage());
            specialLectureRedisClient.compensateSpecialLectureEnrollment(command.specialLectureId());
            throw mapToApplicationException(e);
        }
    }

    /**
     * 2차 방어선 toggle에 따라 DB 저장 경로를 분기한다.
     * <ul>
     *   <li>{@code distributedLock=true}: {@link LockedSpecialLectureEnroller#enrollWithLock}로 락 보호.</li>
     *   <li>{@code distributedLock=false}: {@link SpecialLectureEnrollmentService}로 직행 (시나리오 A baseline).</li>
     * </ul>
     */
    private SpecialLectureEnrollment saveEnrollment(SpecialLectureEnrollmentCreateCommand command) {
        if (defenseProperties.distributedLock()) {
            return lockedSpecialLectureEnroller.enrollWithLock(command.specialLectureId(), command.studentId());
        }
        return specialLectureEnrollmentService.enrollSpecialLectureEnrollment(command.specialLectureId(), command.studentId());
    }

    /**
     * 발생한 예외를 도메인 예외로 변환한다.
     *
     * <ul>
     *   <li>{@link DataIntegrityViolationException} → 4차 방어선 발동 (UK 제약 위반): {@code ALREADY_ENROLLED}</li>
     *   <li>{@link ObjectOptimisticLockingFailureException} (toggle 활성 시) → 3차 방어선 발동:
     *       {@code SPECIAL_LECTURE_ENROLLMENT_FAILED} — 분산 락이 비정상으로 풀렸거나 미적용일 때만 발생</li>
     *   <li>{@link ApplicationException} → 그대로 전파 (도메인 예외 또는 락 획득 실패 등)</li>
     *   <li>그 외 → {@code SPECIAL_LECTURE_ENROLLMENT_FAILED}</li>
     * </ul>
     */
    private RuntimeException mapToApplicationException(Exception e) {
        return switch (e) {
            case DataIntegrityViolationException ignored ->
                    new ApplicationException(SpecialLectureEnrollmentExceptionCode.ALREADY_ENROLLED);
            case ObjectOptimisticLockingFailureException ignored when defenseProperties.optimisticLockHandling() ->
                    new ApplicationException(SpecialLectureEnrollmentExceptionCode.SPECIAL_LECTURE_ENROLLMENT_FAILED);
            case ApplicationException applicationException ->
                    applicationException;
            default ->
                    new ApplicationException(SpecialLectureEnrollmentExceptionCode.SPECIAL_LECTURE_ENROLLMENT_FAILED);
        };
    }
}
