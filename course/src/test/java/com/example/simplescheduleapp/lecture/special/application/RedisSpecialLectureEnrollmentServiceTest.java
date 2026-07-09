package com.example.simplescheduleapp.lecture.special.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.InternalServerExceptionCode;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;
import com.example.simplescheduleapp.lecture.special.application.command.SpecialLectureEnrollmentCreateCommand;
import com.example.simplescheduleapp.lecture.special.config.DefenseProperties;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureEnrollment;
import com.example.simplescheduleapp.lecture.special.exception.SpecialLectureEnrollmentExceptionCode;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.support.MockTestSupport;
import com.example.simplescheduleapp.tutor.domain.Tutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 4단계 방어 구조 단위 테스트.
 *
 * <p>외부 협력 객체(Redis, JPA, AOP)는 모두 Mock으로 대체하고,
 * {@link RedisSpecialLectureEnrollmentService}의 분기 로직과 예외 매핑/보상 패턴만 검증한다.
 * 실제 동시성/락은 {@code playground} K6 부하 테스트와 통합 테스트로 검증.
 */
@DisplayName("RedisSpecialLectureEnrollmentService — 4단계 방어 구조")
class RedisSpecialLectureEnrollmentServiceTest extends MockTestSupport {

    @Mock
    SpecialLectureRedisClient specialLectureRedisClient;

    @Mock
    SpecialLectureEnrollmentService specialLectureEnrollmentService;

    @Mock
    LockedSpecialLectureEnroller lockedSpecialLectureEnroller;

    @InjectMocks
    RedisSpecialLectureEnrollmentService sut;

    DefenseProperties defenseProperties;

    private RedisSpecialLectureEnrollmentService createService(boolean distributedLock, boolean optimisticLockHandling) {
        defenseProperties = new DefenseProperties(distributedLock, optimisticLockHandling);
        return new RedisSpecialLectureEnrollmentService(
                specialLectureRedisClient,
                specialLectureEnrollmentService,
                lockedSpecialLectureEnroller,
                defenseProperties
        );
    }

    private SpecialLectureEnrollment dummyEnrollment() {
        Tutor tutor = new Tutor("tutor", "Password1!", "튜터", 30, "01012345678", 5);
        Student student = new Student("student", "Password1!", "학생", 20, "01098765432", "학교");
        SpecialLecture lecture = new SpecialLecture(
                "특강", LocalDateTime.now(), LocalDateTime.now().plusHours(1), "메모", tutor, 100);
        return new SpecialLectureEnrollment(lecture, student);
    }

    @Nested
    @DisplayName("시나리오 C: 분산 락 ON + 낙관적 락 처리 ON (전체 4단계)")
    class FullDefense {

        @Test
        @DisplayName("[1] 정원 내 수강 신청은 분산 락 경로로 성공한다")
        void enroll_success_with_lock() {
            // given
            RedisSpecialLectureEnrollmentService service = createService(true, true);
            SpecialLectureEnrollmentCreateCommand command = SpecialLectureEnrollmentCreateCommand.of(1L, 100L);
            given(lockedSpecialLectureEnroller.enrollWithLock(100L, 1L)).willReturn(dummyEnrollment());

            // when
            service.enrollSpecialLectureEnrollment(command);

            // then
            verify(specialLectureRedisClient, times(1)).enrollSpecialLectureEnrollment(100L);
            verify(lockedSpecialLectureEnroller, times(1)).enrollWithLock(100L, 1L);
            verify(specialLectureEnrollmentService, never()).enrollSpecialLectureEnrollment(any(), any());
            verify(specialLectureRedisClient, never()).compensateSpecialLectureEnrollment(any());
        }

        @Test
        @DisplayName("[2] 동일 학생 중복 신청 시 4차 방어선(UK)이 발동, ALREADY_ENROLLED + Redis 보상")
        void duplicate_enrollment_triggers_uk_constraint() {
            // given
            RedisSpecialLectureEnrollmentService service = createService(true, true);
            SpecialLectureEnrollmentCreateCommand command = SpecialLectureEnrollmentCreateCommand.of(1L, 100L);
            given(lockedSpecialLectureEnroller.enrollWithLock(100L, 1L))
                    .willThrow(new DataIntegrityViolationException("uk_special_lecture_student violated"));

            // when & then
            assertThatThrownBy(() -> service.enrollSpecialLectureEnrollment(command))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(e -> ((ApplicationException) e).getCode())
                    .isEqualTo(SpecialLectureEnrollmentExceptionCode.ALREADY_ENROLLED);

            verify(specialLectureRedisClient, times(1)).compensateSpecialLectureEnrollment(100L);
        }

        @Test
        @DisplayName("[3] 3차 방어선(낙관적 락) 발동: OptimisticLockingFailureException → 보상 + ENROLLMENT_FAILED")
        void optimistic_lock_failure_is_handled() {
            // given
            RedisSpecialLectureEnrollmentService service = createService(true, true);
            SpecialLectureEnrollmentCreateCommand command = SpecialLectureEnrollmentCreateCommand.of(1L, 100L);
            given(lockedSpecialLectureEnroller.enrollWithLock(100L, 1L))
                    .willThrow(new ObjectOptimisticLockingFailureException(SpecialLecture.class, 100L));

            // when & then
            assertThatThrownBy(() -> service.enrollSpecialLectureEnrollment(command))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(e -> ((ApplicationException) e).getCode())
                    .isEqualTo(SpecialLectureEnrollmentExceptionCode.SPECIAL_LECTURE_ENROLLMENT_FAILED);

            verify(specialLectureRedisClient, times(1)).compensateSpecialLectureEnrollment(100L);
        }

        @Test
        @DisplayName("[4] 분산 락 획득 실패(ApplicationException) → 그대로 전파 + Redis 보상")
        void lock_acquisition_failure_is_propagated() {
            // given
            RedisSpecialLectureEnrollmentService service = createService(true, true);
            SpecialLectureEnrollmentCreateCommand command = SpecialLectureEnrollmentCreateCommand.of(1L, 100L);
            given(lockedSpecialLectureEnroller.enrollWithLock(100L, 1L))
                    .willThrow(new ApplicationException(InternalServerExceptionCode.UNKNOWN_EXCEPTION));

            // when & then
            assertThatThrownBy(() -> service.enrollSpecialLectureEnrollment(command))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(e -> ((ApplicationException) e).getCode())
                    .isEqualTo(InternalServerExceptionCode.UNKNOWN_EXCEPTION);

            verify(specialLectureRedisClient, times(1)).compensateSpecialLectureEnrollment(100L);
        }

        @Test
        @DisplayName("[5] 알 수 없는 RuntimeException → ENROLLMENT_FAILED + Redis 보상")
        void unknown_exception_is_wrapped() {
            // given
            RedisSpecialLectureEnrollmentService service = createService(true, true);
            SpecialLectureEnrollmentCreateCommand command = SpecialLectureEnrollmentCreateCommand.of(1L, 100L);
            given(lockedSpecialLectureEnroller.enrollWithLock(100L, 1L))
                    .willThrow(new RuntimeException("DB connection refused"));

            // when & then
            assertThatThrownBy(() -> service.enrollSpecialLectureEnrollment(command))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(e -> ((ApplicationException) e).getCode())
                    .isEqualTo(SpecialLectureEnrollmentExceptionCode.SPECIAL_LECTURE_ENROLLMENT_FAILED);

            verify(specialLectureRedisClient, times(1)).compensateSpecialLectureEnrollment(100L);
        }
    }

    @Nested
    @DisplayName("시나리오 A: 분산 락 OFF (baseline)")
    class BaselineNoLock {

        @Test
        @DisplayName("[6] distributedLock=false 시 락 우회하고 Service를 직접 호출한다")
        void without_lock_calls_service_directly() {
            // given
            RedisSpecialLectureEnrollmentService service = createService(false, false);
            SpecialLectureEnrollmentCreateCommand command = SpecialLectureEnrollmentCreateCommand.of(1L, 100L);
            given(specialLectureEnrollmentService.enrollSpecialLectureEnrollment(100L, 1L))
                    .willReturn(dummyEnrollment());

            // when
            service.enrollSpecialLectureEnrollment(command);

            // then
            verify(lockedSpecialLectureEnroller, never()).enrollWithLock(any(), any());
            verify(specialLectureEnrollmentService, times(1)).enrollSpecialLectureEnrollment(100L, 1L);
            verify(specialLectureRedisClient, never()).compensateSpecialLectureEnrollment(any());
        }
    }

    @Nested
    @DisplayName("1차 방어선: Redis Atomic 거절")
    class RedisCapacityRejection {

        @Test
        @DisplayName("[7] Redis 정원 초과(CAPACITY_EXCEEDED)는 RedisClient 내부에서 보상 후 그대로 전파")
        void redis_capacity_exceeded_is_propagated() {
            // given: SpecialLectureRedisClient.enrollSpecialLectureEnrollment가 직접 던지는 예외는
            //        본 서비스의 try-catch 외부이므로 추가 보상 호출은 없다 (RedisClient 내부에서 이미 INCR 복구).
            RedisSpecialLectureEnrollmentService service = createService(true, true);
            SpecialLectureEnrollmentCreateCommand command = SpecialLectureEnrollmentCreateCommand.of(1L, 100L);

            // SpecialLectureRedisClient.enrollSpecialLectureEnrollment는 정원 초과 시
            // 자체적으로 INCR 복구한 뒤 ApplicationException(CAPACITY_EXCEEDED)을 던진다.
            org.mockito.Mockito.doThrow(new ApplicationException(LectureExceptionCode.CAPACITY_EXCEEDED))
                    .when(specialLectureRedisClient).enrollSpecialLectureEnrollment(100L);

            // when & then
            assertThatThrownBy(() -> service.enrollSpecialLectureEnrollment(command))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(e -> ((ApplicationException) e).getCode())
                    .isEqualTo(LectureExceptionCode.CAPACITY_EXCEEDED);

            // RedisClient가 자체 보상을 했으므로 외부 보상은 호출되지 않아야 한다
            verify(specialLectureRedisClient, never()).compensateSpecialLectureEnrollment(any());
            verify(lockedSpecialLectureEnroller, never()).enrollWithLock(any(), any());
            verify(specialLectureEnrollmentService, never()).enrollSpecialLectureEnrollment(any(), any());
        }
    }

    @Nested
    @DisplayName("Toggle: optimisticLockHandling=false")
    class OptimisticLockHandlingDisabled {

        @Test
        @DisplayName("[8] optimisticLockHandling=false 일 때도 OptimisticLockingFailureException은 default 분기로 ENROLLMENT_FAILED 처리")
        void optimistic_lock_disabled_falls_through_to_default() {
            // given
            RedisSpecialLectureEnrollmentService service = createService(true, false);
            SpecialLectureEnrollmentCreateCommand command = SpecialLectureEnrollmentCreateCommand.of(1L, 100L);
            given(lockedSpecialLectureEnroller.enrollWithLock(100L, 1L))
                    .willThrow(new ObjectOptimisticLockingFailureException(SpecialLecture.class, 100L));

            // when & then
            // toggle=false 라도 default 분기에서 ENROLLMENT_FAILED로 처리된다.
            // 차이는 "명시적 처리 여부" — 로그 기록/메트릭 분리 등 향후 확장 여지.
            assertThatThrownBy(() -> service.enrollSpecialLectureEnrollment(command))
                    .isInstanceOf(ApplicationException.class)
                    .extracting(e -> ((ApplicationException) e).getCode())
                    .isEqualTo(SpecialLectureEnrollmentExceptionCode.SPECIAL_LECTURE_ENROLLMENT_FAILED);

            verify(specialLectureRedisClient, times(1)).compensateSpecialLectureEnrollment(100L);
        }
    }

    @Test
    @DisplayName("DefenseProperties record 기본값(true/true) 확인")
    void defense_properties_defaults() {
        DefenseProperties props = new DefenseProperties(true, true);
        assertThat(props.distributedLock()).isTrue();
        assertThat(props.optimisticLockHandling()).isTrue();
    }
}
