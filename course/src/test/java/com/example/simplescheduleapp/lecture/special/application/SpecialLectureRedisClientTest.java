package com.example.simplescheduleapp.lecture.special.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.redis.counter.AtomicCounter;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;
import com.example.simplescheduleapp.lecture.special.exception.SpecialLectureEnrollmentExceptionCode;
import com.example.simplescheduleapp.support.MockTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 1차 방어선(Redis Atomic Counter) 단위 테스트.
 * <p>
 * Redis 자체의 원자성은 신뢰하므로 본 테스트의 초점은
 * <ul>
 *   <li>키 포맷이 맞게 생성되는지</li>
 *   <li>정원 초과 시 INCR로 복구하는지</li>
 *   <li>TTL이 적용된 set이 호출되는지</li>
 * </ul>
 * 이다.
 */
@DisplayName("SpecialLectureRedisClient — 1차 방어선 (Redis Atomic)")
class SpecialLectureRedisClientTest extends MockTestSupport {

    @Mock
    AtomicCounter atomicCounter;

    @InjectMocks
    SpecialLectureRedisClient sut;

    @Test
    @DisplayName("특강 생성 시 capacity가 TTL과 함께 set 된다")
    void initialize_uses_set_with_ttl() {
        // when
        sut.initializeSpecialLecture(100L, 50);

        // then
        verify(atomicCounter, times(1))
                .set(eq("special_lecture:100:available"), eq(50L), any(Duration.class));
        verify(atomicCounter, never()).set(any(), anyLong());
    }

    @Test
    @DisplayName("정원 내 신청은 DECR 후 통과 (INCR 복구 호출 없음)")
    void enroll_within_capacity_passes() {
        // given
        given(atomicCounter.decrement("special_lecture:100:available")).willReturn(49L);

        // when
        sut.enrollSpecialLectureEnrollment(100L);

        // then
        verify(atomicCounter, times(1)).decrement("special_lecture:100:available");
        verify(atomicCounter, never()).increment(any());
    }

    @Test
    @DisplayName("정원 초과 시 DECR 결과가 음수 → INCR로 복구 + CAPACITY_EXCEEDED 예외")
    void capacity_exceeded_triggers_incr_and_exception() {
        // given
        given(atomicCounter.decrement("special_lecture:100:available")).willReturn(-1L);

        // when & then
        assertThatThrownBy(() -> sut.enrollSpecialLectureEnrollment(100L))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getCode())
                .isEqualTo(LectureExceptionCode.CAPACITY_EXCEEDED);

        // INCR로 복구가 정확히 1번 호출되어야 한다
        verify(atomicCounter, times(1)).increment("special_lecture:100:available");
    }

    @Test
    @DisplayName("Redis가 null을 반환하면 ENROLLMENT_FAILED 예외")
    void null_result_triggers_failure_exception() {
        // given: Redis 연결 문제 등으로 null이 돌아오는 비정상 상황
        given(atomicCounter.decrement("special_lecture:100:available")).willReturn(null);

        // when & then
        assertThatThrownBy(() -> sut.enrollSpecialLectureEnrollment(100L))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getCode())
                .isEqualTo(SpecialLectureEnrollmentExceptionCode.SPECIAL_LECTURE_ENROLLMENT_FAILED);

        verify(atomicCounter, never()).increment(any());
    }

    @Test
    @DisplayName("compensate는 INCR을 그대로 호출한다 (DB 실패 시 외부 보상)")
    void compensate_calls_increment() {
        // when
        sut.compensateSpecialLectureEnrollment(100L);

        // then
        verify(atomicCounter, times(1)).increment("special_lecture:100:available");
    }
}
