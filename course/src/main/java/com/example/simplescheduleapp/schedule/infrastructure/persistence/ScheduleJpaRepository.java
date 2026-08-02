package com.example.simplescheduleapp.schedule.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

// 아래 조회는 전부 네이티브 SQL이라 엔티티 순수화(Schedule → ScheduleEntity)의 영향을 받지 않는다.
// 포트가 List<Long>(ID)만 반환하므로 도메인 매핑도 불필요하다. (ADR-0004)
public interface ScheduleJpaRepository extends JpaRepository<ScheduleEntity, Long> {

    // excludeScheduleId는 항상 non-null 값(제외 없음을 뜻하는 sentinel 포함)으로 바인딩된다.
    // (어댑터가 null -> sentinel 변환을 담당) 이전에는 native 쿼리에서
    // "CAST(:excludeScheduleId AS BIGINT) IS NULL" 형태로 NULL 여부를 직접 검사했는데,
    // BIGINT는 MySQL에서 유효한 CAST 대상 타입이 아니라 SQLSyntaxErrorException을 유발했다.
    // 파라미터를 절대 NULL로 바인딩하지 않는 이 방식은 CAST 자체가 필요 없어
    // MySQL/PostgreSQL/H2 세 엔진 모두에서 동일하게 동작한다.
    @Query(value = """
        SELECT s.schedule_id FROM schedule s
        LEFT JOIN lecture l ON s.schedule_id = l.schedule_id
        LEFT JOIN special_lecture sl ON s.schedule_id = sl.schedule_id
        LEFT JOIN consultation c ON s.schedule_id = c.schedule_id
        WHERE COALESCE(l.tutor_id, sl.tutor_id, c.tutor_id) = :tutorId
          AND s.start_time < :endTime
          AND s.end_time > :startTime
          AND s.deleted_date IS NULL
          AND s.schedule_id != :excludeScheduleId
        """, nativeQuery = true)
    List<Long> findOverlappingScheduleIdsByTutorId(
            @Param("tutorId") Long tutorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeScheduleId") long excludeScheduleId
    );

    @Query(value = """
        SELECT s.schedule_id FROM schedule s
        INNER JOIN lecture_enrollment le ON s.schedule_id = le.lecture_id
        WHERE le.student_id = :studentId
          AND s.start_time < :endTime
          AND s.end_time > :startTime
          AND s.deleted_date IS NULL
          AND s.schedule_id != :excludeScheduleId
        UNION
        SELECT s.schedule_id FROM schedule s
        INNER JOIN special_lecture_enrollment sle ON s.schedule_id = sle.special_lecture_id
        WHERE sle.student_id = :studentId
          AND s.start_time < :endTime
          AND s.end_time > :startTime
          AND s.deleted_date IS NULL
          AND s.schedule_id != :excludeScheduleId
        """, nativeQuery = true)
    List<Long> findOverlappingScheduleIdsByStudentId(
            @Param("studentId") Long studentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeScheduleId") long excludeScheduleId
    );
}
