package com.example.simplescheduleapp.schedule.domain.service;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.schedule.domain.ScheduleRepository;
import com.example.simplescheduleapp.schedule.exception.ScheduleExceptionCode;
import com.example.simplescheduleapp.support.MockTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

class ScheduleConflictValidatorTest extends MockTestSupport {

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private ScheduleConflictValidator scheduleConflictValidator;

    private final LocalDateTime start = LocalDateTime.of(2026, 8, 1, 10, 0);
    private final LocalDateTime end = LocalDateTime.of(2026, 8, 1, 12, 0);

    @Test
    void 튜터_시간_겹침이_있으면_예외가_발생한다() {
        // given
        given(scheduleRepository.findOverlappingScheduleIdsByTutorId(anyLong(), any(), any(), any()))
                .willReturn(List.of(7L));

        // when & then
        assertThatThrownBy(() -> scheduleConflictValidator.validateNoTutorConflict(1L, start, end, null))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", ScheduleExceptionCode.TUTOR_SCHEDULE_CONFLICT);
    }

    @Test
    void 학생_시간_겹침이_있으면_예외가_발생한다() {
        // given
        given(scheduleRepository.findOverlappingScheduleIdsByStudentId(anyLong(), any(), any(), any()))
                .willReturn(List.of(3L));

        // when & then
        assertThatThrownBy(() -> scheduleConflictValidator.validateNoStudentConflict(2L, start, end, null))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", ScheduleExceptionCode.STUDENT_SCHEDULE_CONFLICT);
    }

    @Test
    void 겹침이_없으면_통과한다() {
        // given
        given(scheduleRepository.findOverlappingScheduleIdsByTutorId(anyLong(), any(), any(), any()))
                .willReturn(List.of());
        given(scheduleRepository.findOverlappingScheduleIdsByStudentId(anyLong(), any(), any(), any()))
                .willReturn(List.of());

        // when & then
        assertThatCode(() -> {
            scheduleConflictValidator.validateNoTutorConflict(1L, start, end, 10L);
            scheduleConflictValidator.validateNoStudentConflict(2L, start, end, null);
        }).doesNotThrowAnyException();
    }
}
