package com.example.simplescheduleapp.lecture.special.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.redis.counter.AtomicCounter;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;
import com.example.simplescheduleapp.lecture.special.exception.SpecialLectureExceptionCode;
import com.example.simplescheduleapp.support.MockTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 1차 방어선(Redis Lua check-and-decrement) 단위 테스트.
 * <p>
 * Redis 자체의 원자성은 신뢰하므로 본 테스트의 초점은
 * <ul>
 *   <li>키 포맷이 맞게 생성되는지</li>
 *   <li>키 부재(-2)와 정원 소진(-1)이 구분되어 예외 매핑되는지</li>
 *   <li>TTL이 적용된 set이 호출되는지 (endTime 정렬 + 과거 시각 보정)</li>
 *   <li>보상 경로가 키 부재 시 예외 대신 조용히 넘어가는지 (원인 예외 보존)</li>
 * </ul>
 * 이다. (feature/1-5-redis-lua-atomicity 통합분)
 */
@DisplayName("SpecialLectureRedisClient — 1차 방어선 (Redis Lua)")
class SpecialLectureRedisClientTest extends MockTestSupport {

    @Mock
    AtomicCounter atomicCounter;

    @InjectMocks
    SpecialLectureRedisClient sut;

    @Test
    @DisplayName("특강 생성 시 capacity가 TTL(종료 시각 + 1일)과 함께 set 된다")
    void initialize_uses_set_with_ttl() {
        // when
        sut.initializeSpecialLecture(100L, 50, LocalDateTime.now().plusDays(7));

        // then
        verify(atomicCounter, times(1))
                .set(eq("special_lecture:100:available"), eq(50L), any(Duration.class));
        verify(atomicCounter, never()).set(any(), anyLong());
    }

    @Test
    @DisplayName("종료 시각이 과거여도 TTL 없는 set으로 퇴보하지 않는다 (보정 TTL 적용)")
    void initialize_with_past_end_time_still_sets_ttl() {
        // when
        sut.initializeSpecialLecture(100L, 50, LocalDateTime.now().minusDays(3));

        // then
        verify(atomicCounter, times(1))
                .set(eq("special_lecture:100:available"), eq(50L), any(Duration.class));
        verify(atomicCounter, never()).set(any(), anyLong());
    }

    @Test
    @DisplayName("정원 내 신청은 조건부 감소 후 통과 (복구 왕복 없음)")
    void enroll_within_capacity_passes() {
        // given
        given(atomicCounter.decrementIfPositive("special_lecture:100:available")).willReturn(49L);

        // when
        sut.enrollSpecialLectureEnrollment(100L);

        // then
        verify(atomicCounter, times(1)).decrementIfPositive("special_lecture:100:available");
        verify(atomicCounter, never()).increment(any());
        verify(atomicCounter, never()).incrementIfExists(any());
    }

    @Test
    @DisplayName("정원 소진(-1) 시 감소 없이 CAPACITY_EXCEEDED — INCR 복구가 필요 없다")
    void capacity_exhausted_throws_without_compensation() {
        // given
        given(atomicCounter.decrementIfPositive("special_lecture:100:available")).willReturn(-1L);

        // when & then
        assertThatThrownBy(() -> sut.enrollSpecialLectureEnrollment(100L))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getCode())
                .isEqualTo(LectureExceptionCode.CAPACITY_EXCEEDED);

        // Lua가 감소 자체를 수행하지 않으므로 복구 INCR이 호출되면 안 된다
        verify(atomicCounter, never()).increment(any());
        verify(atomicCounter, never()).incrementIfExists(any());
    }

    @Test
    @DisplayName("키 부재(-2)는 정원 초과가 아니라 SL002(NOT_FOUND_IN_REDIS)로 구분된다")
    void key_missing_throws_not_found_in_redis() {
        // given: 키 만료 또는 미초기화
        given(atomicCounter.decrementIfPositive("special_lecture:100:available")).willReturn(-2L);

        // when & then
        assertThatThrownBy(() -> sut.enrollSpecialLectureEnrollment(100L))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getCode())
                .isEqualTo(SpecialLectureExceptionCode.SPECIAL_LECTURE_NOT_FOUND_IN_REDIS);
    }

    @Test
    @DisplayName("Redis가 null을 반환하면 SL002 예외")
    void null_result_throws_not_found_in_redis() {
        // given: Redis 연결 문제 등으로 null이 돌아오는 비정상 상황
        given(atomicCounter.decrementIfPositive("special_lecture:100:available")).willReturn(null);

        // when & then
        assertThatThrownBy(() -> sut.enrollSpecialLectureEnrollment(100L))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getCode())
                .isEqualTo(SpecialLectureExceptionCode.SPECIAL_LECTURE_NOT_FOUND_IN_REDIS);
    }

    @Test
    @DisplayName("compensate는 키가 존재할 때만 증가시킨다 (DB 실패 시 외부 보상)")
    void compensate_calls_increment_if_exists() {
        // given
        given(atomicCounter.incrementIfExists("special_lecture:100:available")).willReturn(1L);

        // when
        sut.compensateSpecialLectureEnrollment(100L);

        // then
        verify(atomicCounter, times(1)).incrementIfExists("special_lecture:100:available");
        verify(atomicCounter, never()).increment(any());
    }

    @Test
    @DisplayName("compensate 시 키 부재(-2)여도 예외를 던지지 않는다 — 원인이 된 DB 예외를 가리지 않기 위함")
    void compensate_with_missing_key_does_not_throw() {
        // given
        given(atomicCounter.incrementIfExists("special_lecture:100:available")).willReturn(-2L);

        // when & then
        assertThatCode(() -> sut.compensateSpecialLectureEnrollment(100L))
                .doesNotThrowAnyException();
    }
}
