package com.example.simplescheduleapp.schedule.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 스케줄 영속성 포트 (구현: infrastructure JPA 어댑터). 순수 자바 — Spring Data 미참조. (ADR-0002 Stage 2)
 */
public interface ScheduleRepository {

    List<Long> findOverlappingScheduleIdsByTutorId(
            Long tutorId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long excludeScheduleId
    );

    List<Long> findOverlappingScheduleIdsByStudentId(
            Long studentId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long excludeScheduleId
    );
}
