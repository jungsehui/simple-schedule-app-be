package com.example.simplescheduleapp.schedule.application;

import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.schedule.application.port.out.ScheduleQueryPort;
import com.example.simplescheduleapp.support.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * 역할별 질의 분기와 조회 창 검증.
 *
 * <p><b>분기를 여기서 검증하는 이유.</b> 역할을 잘못 태우면 학생에게 강사의 일정이 나간다 —
 * 상태 코드도 200이고 응답 모양도 정상이라 HTTP 테스트로는 드러나지 않는다. 어떤 포트 메서드가
 * 불렸는지가 유일한 증거다.
 */
@DisplayName("캘린더 조회(서비스) 은(는)")
class ScheduleQueryServiceTest extends UnitTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2032, 5, 1, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2032, 5, 31, 0, 0);
    private static final Long MEMBER_ID = 5L;

    @InjectMocks
    private ScheduleQueryService scheduleQueryService;

    @Mock
    private ScheduleQueryPort scheduleQueryPort;

    @DisplayName("강사는 강사 질의만 탄다")
    @Test
    void 강사는_강사_질의만_탄다() {
        scheduleQueryService.findMySchedules(MEMBER_ID, Role.TUTOR, FROM, TO);

        verify(scheduleQueryPort).findTutorSchedules(MEMBER_ID, FROM, TO);
        verifyNoMoreInteractions(scheduleQueryPort);
    }

    @DisplayName("학생은 학생 질의만 탄다")
    @Test
    void 학생은_학생_질의만_탄다() {
        scheduleQueryService.findMySchedules(MEMBER_ID, Role.STUDENT, FROM, TO);

        verify(scheduleQueryPort).findStudentSchedules(MEMBER_ID, FROM, TO);
        verifyNoMoreInteractions(scheduleQueryPort);
    }

    @DisplayName("학부모는 학부모 질의만 탄다")
    @Test
    void 학부모는_학부모_질의만_탄다() {
        scheduleQueryService.findMySchedules(MEMBER_ID, Role.PARENT, FROM, TO);

        verify(scheduleQueryPort).findParentSchedules(MEMBER_ID, FROM, TO);
        verifyNoMoreInteractions(scheduleQueryPort);
    }

    @DisplayName("시작이 종료보다 뒤면 400(SC2)이고 DB를 치지 않는다")
    @ParameterizedTest(name = "role={0}")
    @EnumSource(Role.class)
    void 뒤집힌_창은_400이다(Role role) {
        assertThatThrownBy(() -> scheduleQueryService.findMySchedules(MEMBER_ID, role, TO, FROM))
                .isInstanceOf(ApplicationException.class)
                .satisfies(e -> assertThat(((ApplicationException) e).getCode().getCode()).isEqualTo("SC2"));

        verifyNoMoreInteractions(scheduleQueryPort);
    }

    @DisplayName("시작과 종료가 같아도 400이다 — 빈 구간을 조회할 이유가 없다")
    @Test
    void 같은_시각_창은_400이다() {
        assertThatThrownBy(() -> scheduleQueryService.findMySchedules(MEMBER_ID, Role.STUDENT, FROM, FROM))
                .isInstanceOf(ApplicationException.class)
                .satisfies(e -> assertThat(((ApplicationException) e).getCode().getCode()).isEqualTo("SC2"));
    }

    @DisplayName("창이 상한을 넘으면 400(SC3)이고 DB를 치지 않는다")
    @Test
    void 너무_넓은_창은_400이다() {
        assertThatThrownBy(() -> scheduleQueryService.findMySchedules(
                MEMBER_ID, Role.TUTOR, FROM, FROM.plusDays(367)))
                .isInstanceOf(ApplicationException.class)
                .satisfies(e -> assertThat(((ApplicationException) e).getCode().getCode()).isEqualTo("SC3"));

        verifyNoMoreInteractions(scheduleQueryPort);
    }

    @DisplayName("상한 경계값(366일)은 통과한다")
    @Test
    void 상한_경계값은_통과한다() {
        scheduleQueryService.findMySchedules(MEMBER_ID, Role.TUTOR, FROM, FROM.plusDays(366));

        verify(scheduleQueryPort).findTutorSchedules(MEMBER_ID, FROM, FROM.plusDays(366));
    }
}
