package com.example.simplescheduleapp.schedule.infrastructure.persistence;

import com.example.simplescheduleapp.schedule.domain.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@code ScheduleRepository} 포트의 JPA 어댑터. (ADR-0002 Stage 2)
 */
@Repository
@RequiredArgsConstructor
public class ScheduleRepositoryAdapter implements ScheduleRepository {

    private final ScheduleJpaRepository jpaRepository;

    @Override
    public List<Long> findOverlappingScheduleIdsByTutorId(
            Long tutorId, LocalDateTime startTime, LocalDateTime endTime, Long excludeScheduleId) {
        return jpaRepository.findOverlappingScheduleIdsByTutorId(tutorId, startTime, endTime, excludeScheduleId);
    }

    @Override
    public List<Long> findOverlappingScheduleIdsByStudentId(
            Long studentId, LocalDateTime startTime, LocalDateTime endTime, Long excludeScheduleId) {
        return jpaRepository.findOverlappingScheduleIdsByStudentId(studentId, startTime, endTime, excludeScheduleId);
    }
}
