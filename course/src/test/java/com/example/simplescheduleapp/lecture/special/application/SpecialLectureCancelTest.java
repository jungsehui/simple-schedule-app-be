package com.example.simplescheduleapp.lecture.special.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.special.application.command.SpecialLectureEnrollmentCreateCommand;
import com.example.simplescheduleapp.lecture.special.exception.SpecialLectureEnrollmentExceptionCode;
import com.example.simplescheduleapp.support.MockTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 특강 수강신청 취소 — 좌석 반환의 순서와 멱등성.
 *
 * <p>이 두 가지가 이 기능에서 틀리기 가장 쉬운 지점이라 각각을 테스트가 고정한다.
 *
 * <p><b>순서</b>: DB 삭제가 먼저, 좌석 반환이 나중이다. 뒤집으면 DB 삭제 실패 시 좌석이 늘어
 * 초과 판매가 된다. 반대 방향(DB 먼저)은 Redis 증가가 실패해도 좌석이 하나 덜 팔릴 뿐이다.
 *
 * <p><b>멱등성</b>: 삭제된 행 수가 유일한 판정 근거다. 0행이면 좌석을 돌려주지 않는다.
 * 조회 후 삭제였다면 동시 취소 두 건이 같은 행을 읽고 둘 다 좌석을 반환해 정원을 넘긴다.
 */
@DisplayName("특강 수강신청 취소 은(는)")
class SpecialLectureCancelTest extends MockTestSupport {

    @Mock
    SpecialLectureRedisClient specialLectureRedisClient;

    @Mock
    SpecialLectureEnrollmentService specialLectureEnrollmentService;

    private final SpecialLectureEnrollmentCreateCommand command =
            SpecialLectureEnrollmentCreateCommand.of(7L, 100L);

    private RedisSpecialLectureEnrollmentService sut() {
        return new RedisSpecialLectureEnrollmentService(
                specialLectureRedisClient, specialLectureEnrollmentService, null, null);
    }

    @DisplayName("한 행을 지웠으면 좌석을 돌려준다")
    @Test
    void 삭제되면_좌석을_돌려준다() {
        given(specialLectureEnrollmentService.cancelSpecialLectureEnrollment(100L, 7L)).willReturn(1);

        sut().cancelSpecialLectureEnrollment(command);

        verify(specialLectureRedisClient).compensateSpecialLectureEnrollment(100L);
    }

    /**
     * <b>이 테스트가 순서를 고정한다.</b> 좌석 반환이 삭제보다 먼저 일어나면, 삭제가 실패했을 때
     * 존재하는 신청의 좌석이 풀려 정원을 넘긴다.
     */
    @DisplayName("좌석 반환은 반드시 DB 삭제 뒤에 일어난다")
    @Test
    void 좌석_반환은_삭제_뒤다() {
        given(specialLectureEnrollmentService.cancelSpecialLectureEnrollment(100L, 7L)).willReturn(1);

        sut().cancelSpecialLectureEnrollment(command);

        inOrder(specialLectureEnrollmentService, specialLectureRedisClient)
                .verify(specialLectureEnrollmentService).cancelSpecialLectureEnrollment(100L, 7L);
        inOrder(specialLectureEnrollmentService, specialLectureRedisClient)
                .verify(specialLectureRedisClient).compensateSpecialLectureEnrollment(100L);
    }

    /**
     * <b>이 테스트가 멱등성을 고정한다.</b> 두 번째 취소는 0행을 받는다. 그때 좌석을 또
     * 돌려주면 정원이 늘어난다 — 취소 한 번에 좌석 두 개가 풀리는 셈이다.
     */
    @DisplayName("지운 행이 없으면 좌석을 돌려주지 않는다 — 두 번째 취소가 정원을 늘리지 못한다")
    @Test
    void 지운_행이_없으면_좌석을_안_돌려준다() {
        given(specialLectureEnrollmentService.cancelSpecialLectureEnrollment(100L, 7L)).willReturn(0);

        assertThatThrownBy(() -> sut().cancelSpecialLectureEnrollment(command))
                .isInstanceOf(ApplicationException.class)
                .extracting(e -> ((ApplicationException) e).getCode())
                .isEqualTo(SpecialLectureEnrollmentExceptionCode.ENROLLMENT_NOT_FOUND);

        verify(specialLectureRedisClient, never()).compensateSpecialLectureEnrollment(100L);
    }

    @DisplayName("취소 대상은 커맨드의 학생과 특강이다 — 다른 조합으로 지우지 않는다")
    @Test
    void 취소_대상이_정확하다() {
        given(specialLectureEnrollmentService.cancelSpecialLectureEnrollment(100L, 7L)).willReturn(1);

        sut().cancelSpecialLectureEnrollment(command);

        verify(specialLectureEnrollmentService).cancelSpecialLectureEnrollment(100L, 7L);
    }
}
