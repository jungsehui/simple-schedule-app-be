package com.example.simplescheduleapp.lecture.special.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.redis.counter.AtomicCounter;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLectureRepository;
import com.example.simplescheduleapp.lecture.special.exception.SpecialLectureEnrollmentExceptionCode;
import com.example.simplescheduleapp.lecture.special.exception.SpecialLectureExceptionCode;
import com.example.simplescheduleapp.support.MockTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 신청 마감(종료된 특강 차단) — 1차 방어선에서의 처리.
 *
 * <p><b>왜 Redis에서 거르는가.</b> 신청 경로는 DB를 치기 전에 Redis로 정원을 거른다. 그것이
 * 1차 방어선의 존재 이유다(1만 명이 몰려도 9,900명은 DB에 닿지 않는다). 시간 검증을 DB 로드
 * 뒤에 두면 그 설계가 깨지므로, 마감 시각도 정원과 같은 층에 캐시해 Lua가 함께 본다.
 *
 * <p><b>키 부재는 DB로 판별한다.</b> 마감 키까지 TTL로 사라진 뒤에는 Redis만으로 "끝나서 없음"과
 * "정원 정보를 잃음"을 구분할 수 없다. 그때만 특강을 읽어 가른다 — 정상 신청은 DB를 읽지 않는다.
 */
@DisplayName("특강 신청 마감 은(는)")
class SpecialLectureDeadlineTest extends MockTestSupport {

    private static final String CAPACITY_KEY = "special_lecture:100:available";
    private static final String DEADLINE_KEY = "special_lecture:100:deadline";

    @Mock
    AtomicCounter atomicCounter;

    @Mock
    SpecialLectureRepository specialLectureRepository;

    @InjectMocks
    SpecialLectureRedisClient sut;

    private SpecialLecture lectureEndingAt(LocalDateTime endTime) {
        return new SpecialLecture("특강", endTime.minusHours(2), endTime, "메모", 9L, 50);
    }

    @DisplayName("생성 시 마감 시각을 정원과 함께 캐시한다 — 이게 있어야 거절이 DB를 안 읽는다")
    @Test
    void 생성_시_마감_시각도_캐시한다() {
        sut.initializeSpecialLecture(100L, 50, LocalDateTime.now().plusDays(7));

        verify(atomicCounter, times(1)).set(eq(CAPACITY_KEY), eq(50L), any(Duration.class));
        verify(atomicCounter, times(1)).set(eq(DEADLINE_KEY), anyLong(), any(Duration.class));
        // TTL 없는 set으로 퇴보하지 않는다
        verify(atomicCounter, never()).set(anyString(), anyLong());
    }

    @DisplayName("마감이 지나면 SLE005다 — DB를 읽지 않는다")
    @Test
    void 마감이_지나면_SLE005다() {
        given(atomicCounter.decrementIfPositiveBefore(eq(CAPACITY_KEY), eq(DEADLINE_KEY), anyLong()))
                .willReturn(-3L);

        assertThatThrownBy(() -> sut.enrollSpecialLectureEnrollment(100L))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getCode())
                .isEqualTo(SpecialLectureEnrollmentExceptionCode.SPECIAL_LECTURE_ENDED);

        // 정상 거절 경로이므로 DB를 읽을 이유가 없다
        verify(specialLectureRepository, never()).findById(any());
    }

    @DisplayName("키가 없고 특강이 이미 끝났으면 종료됨이다 — 정상 상황이다")
    @Test
    void 키_부재_종료된_특강은_SLE005다() {
        given(atomicCounter.decrementIfPositiveBefore(eq(CAPACITY_KEY), eq(DEADLINE_KEY), anyLong()))
                .willReturn(-2L);
        given(specialLectureRepository.findById(100L))
                .willReturn(Optional.of(lectureEndingAt(LocalDateTime.now().minusDays(3))));

        assertThatThrownBy(() -> sut.enrollSpecialLectureEnrollment(100L))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getCode())
                .isEqualTo(SpecialLectureEnrollmentExceptionCode.SPECIAL_LECTURE_ENDED);
    }

    /**
     * 여기가 진짜 이상 상황이다. 진행 중인 특강인데 정원 정보가 없다 — 초기화 누락이거나 유실이다.
     * 종전에는 위의 정상 상황과 같은 코드로 나가 알람에 묻혔다.
     */
    @DisplayName("키가 없는데 특강이 진행 중이면 SL002다 — 이건 버그 신호다")
    @Test
    void 키_부재_진행중_특강은_SL002다() {
        given(atomicCounter.decrementIfPositiveBefore(eq(CAPACITY_KEY), eq(DEADLINE_KEY), anyLong()))
                .willReturn(-2L);
        given(specialLectureRepository.findById(100L))
                .willReturn(Optional.of(lectureEndingAt(LocalDateTime.now().plusDays(3))));

        assertThatThrownBy(() -> sut.enrollSpecialLectureEnrollment(100L))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getCode())
                .isEqualTo(SpecialLectureExceptionCode.SPECIAL_LECTURE_CAPACITY_UNAVAILABLE);
    }

    @DisplayName("키도 없고 특강도 없으면 특강 없음이다")
    @Test
    void 키_부재_특강_부재는_SLE002다() {
        given(atomicCounter.decrementIfPositiveBefore(eq(CAPACITY_KEY), eq(DEADLINE_KEY), anyLong()))
                .willReturn(-2L);
        given(specialLectureRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.enrollSpecialLectureEnrollment(100L))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getCode())
                .isEqualTo(SpecialLectureEnrollmentExceptionCode.SPECIAL_LECTURE_NOT_FOUND);
    }

    @DisplayName("마감 전 정상 신청은 DB를 읽지 않는다")
    @Test
    void 정상_신청은_DB를_읽지_않는다() {
        given(atomicCounter.decrementIfPositiveBefore(eq(CAPACITY_KEY), eq(DEADLINE_KEY), anyLong()))
                .willReturn(49L);

        sut.enrollSpecialLectureEnrollment(100L);

        verify(specialLectureRepository, never()).findById(any());
    }
}
