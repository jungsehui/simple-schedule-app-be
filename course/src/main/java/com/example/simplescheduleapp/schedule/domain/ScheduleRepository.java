package com.example.simplescheduleapp.schedule.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query(value = """
        SELECT s.schedule_id FROM schedule s
        LEFT JOIN lecture l ON s.schedule_id = l.schedule_id
        LEFT JOIN special_lecture sl ON s.schedule_id = sl.schedule_id
        LEFT JOIN consultation c ON s.schedule_id = c.schedule_id
        WHERE COALESCE(l.tutor_id, sl.tutor_id, c.tutor_id) = :tutorId
          AND s.start_time < :endTime
          AND s.end_time > :startTime
          AND s.deleted_date IS NULL
          AND (:excludeScheduleId IS NULL OR s.schedule_id != :excludeScheduleId)
        """, nativeQuery = true)
    List<Long> findOverlappingScheduleIdsByTutorId(
            Long tutorId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long excludeScheduleId
    );

    @Query(value = """
        SELECT s.schedule_id FROM schedule s
        INNER JOIN lecture_enrollment le ON s.schedule_id = le.lecture_id
        WHERE le.student_id = :studentId
          AND s.start_time < :endTime
          AND s.end_time > :startTime
          AND s.deleted_date IS NULL
          AND (:excludeScheduleId IS NULL OR s.schedule_id != :excludeScheduleId)
        UNION
        SELECT s.schedule_id FROM schedule s
        INNER JOIN special_lecture_enrollment sle ON s.schedule_id = sle.special_lecture_id
        WHERE sle.student_id = :studentId
          AND s.start_time < :endTime
          AND s.end_time > :startTime
          AND s.deleted_date IS NULL
          AND (:excludeScheduleId IS NULL OR s.schedule_id != :excludeScheduleId)
        """, nativeQuery = true)
    List<Long> findOverlappingScheduleIdsByStudentId(
            Long studentId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long excludeScheduleId
    );
}
