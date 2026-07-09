package com.example.simplescheduleapp.lecture.special.application;

import com.example.simplescheduleapp.common.redis.lock.RedissonDistributedLock;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureEnrollment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 2차 방어선: Redisson FairLock으로 critical section을 직렬화한다.
 * <p>
 * <h3>왜 별도 컴포넌트인가</h3>
 * Spring AOP는 프록시 기반이므로 <b>같은 빈 내부의 self-invocation에는 적용되지 않는다</b>.
 * {@link RedissonDistributedLock}이 발동하려면 외부 빈에서 메서드를 호출해야 한다.
 * 따라서 락 적용 메서드만 별도 빈으로 분리했다.
 *
 * <h3>락 키 설계</h3>
 * <pre>
 * LOCK:special_lecture:{specialLectureId}:enroll
 * </pre>
 * 특강 단위로 락을 분리하여, 서로 다른 특강의 신청은 직렬화되지 않는다.
 *
 * <h3>FairLock을 쓰는 이유</h3>
 * 일반 Lock은 대기 큐에서 임의 스레드가 다음 락을 획득(기아 가능).
 * FairLock은 FIFO 순서를 보장하므로, 선착순 시스템의 공정성과 부합한다.
 *
 * <h3>트랜잭션 경계</h3>
 * {@link com.example.simplescheduleapp.common.redis.lock.RedissonDistributedLockAop}는
 * 내부적으로 {@code REQUIRES_NEW} 전파로 별도 트랜잭션을 시작한다.
 * 즉, 락을 잡고 있는 동안 트랜잭션을 시작-커밋한 뒤 락을 푼다.
 * 이로써 "락 해제 후에 트랜잭션이 커밋되는" race condition을 차단한다.
 *
 * <h3>1차 + 2차 협력</h3>
 * 정원 100명 특강에 10,000명이 동시 요청해도,
 * 1차(Redis Atomic)에서 9,900명이 즉시 거절되고 약 100명만 이 메서드에 도달한다.
 * 따라서 직렬화로 인한 성능 저하가 수용 가능한 수준이다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class LockedSpecialLectureEnroller {

    private final SpecialLectureEnrollmentService specialLectureEnrollmentService;

    /**
     * 분산 락을 잡고 특강 수강 신청을 수행한다.
     *
     * <p>락 획득 실패(waitTime 초과) 시 AOP가 {@code ApplicationException}을 던지며,
     * 호출자({@link RedisSpecialLectureEnrollmentService})가 보상 트랜잭션으로 Redis 카운터를 복구한다.
     */
    @RedissonDistributedLock(
            key = "'special_lecture:' + #specialLectureId + ':enroll'",
            waitTime = 5L,
            leaseTime = 3L
    )
    public SpecialLectureEnrollment enrollWithLock(Long specialLectureId, Long studentId) {
        return specialLectureEnrollmentService.enrollSpecialLectureEnrollment(specialLectureId, studentId);
    }
}
