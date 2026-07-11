package com.example.simplescheduleapp.schedule.domain.service;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.schedule.domain.ScheduleRepository;
import com.example.simplescheduleapp.schedule.exception.ScheduleExceptionCode;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 튜터/학생 시간 겹침 검증 도메인 서비스. 순수 자바 — 빈 등록은 {@code config.ScheduleConfig}. (ADR-0002)
 */
@RequiredArgsConstructor
public class ScheduleConflictValidator {

    private final ScheduleRepository scheduleRepository;

    public void validateNoTutorConflict(Long tutorId, LocalDateTime startTime, LocalDateTime endTime, Long excludeScheduleId) {
        List<Long> conflicts = scheduleRepository.findOverlappingScheduleIdsByTutorId(
                tutorId, startTime, endTime, excludeScheduleId
        );
        if (!conflicts.isEmpty()) {
            throw new ApplicationException(ScheduleExceptionCode.TUTOR_SCHEDULE_CONFLICT);
        }
    }

    public void validateNoStudentConflict(Long studentId, LocalDateTime startTime, LocalDateTime endTime, Long excludeScheduleId) {
        List<Long> conflicts = scheduleRepository.findOverlappingScheduleIdsByStudentId(
                studentId, startTime, endTime, excludeScheduleId
        );
        if (!conflicts.isEmpty()) {
            throw new ApplicationException(ScheduleExceptionCode.STUDENT_SCHEDULE_CONFLICT);
        }
    }
}
