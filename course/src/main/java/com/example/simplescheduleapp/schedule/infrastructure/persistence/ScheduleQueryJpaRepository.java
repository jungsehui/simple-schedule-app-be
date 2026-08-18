package com.example.simplescheduleapp.schedule.infrastructure.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 캘린더 조회용 네이티브 질의.
 *
 * <p>쓰기용 {@code ScheduleJpaRepository}와 분리한다 — 저쪽은 불변식 검사를 위해 ID만 돌려주고,
 * 이쪽은 화면을 채운다. {@code Repository} 마커만 확장해 CRUD 메서드를 노출하지 않는다.
 *
 * <p><b>겹침 판정은 {@code start &lt; to AND end &gt; from}</b>이다. 창에 걸친 일정(창 시작 전에
 * 시작해 창 안까지 이어지는 것)도 포함해야 캘린더에 구멍이 안 생긴다. 기존 충돌 검사 질의와
 * 같은 술어를 쓴다.
 *
 * <p><b>파생 컬럼 별칭은 스네이크로 둔다.</b> PostgreSQL은 따옴표 없는 별칭을 소문자로 접기
 * 때문에 {@code AS scheduleId}는 {@code scheduleid}가 된다. 인터페이스 프로젝션이 세 엔진에서
 * 같게 동작하도록 원래 컬럼명을 그대로 노출한다.
 */
public interface ScheduleQueryJpaRepository extends Repository<ScheduleEntity, Long> {

    @Query(value = """
        SELECT s.schedule_id, s.type, s.title, s.start_time, s.end_time, s.memo
        FROM schedule s
        LEFT JOIN lecture l ON s.schedule_id = l.schedule_id
        LEFT JOIN special_lecture sl ON s.schedule_id = sl.schedule_id
        LEFT JOIN consultation c ON s.schedule_id = c.schedule_id
        WHERE COALESCE(l.tutor_id, sl.tutor_id, c.tutor_id) = :tutorId
          AND s.start_time < :to
          AND s.end_time > :from
          AND s.deleted_date IS NULL
        ORDER BY s.start_time
        """, nativeQuery = true)
    List<ScheduleRow> findTutorSchedules(
            @Param("tutorId") Long tutorId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // 일반 수강과 특강 수강은 서로 다른 테이블이라 UNION으로 합친다.
    // UNION은 중복도 제거하지만 두 집합은 원래 겹치지 않는다 — 합치는 것이 목적이다.
    @Query(value = """
        SELECT s.schedule_id, s.type, s.title, s.start_time, s.end_time, s.memo
        FROM schedule s
        INNER JOIN lecture_enrollment le ON s.schedule_id = le.lecture_id
        WHERE le.student_id = :studentId
          AND s.start_time < :to
          AND s.end_time > :from
          AND s.deleted_date IS NULL
        UNION
        SELECT s.schedule_id, s.type, s.title, s.start_time, s.end_time, s.memo
        FROM schedule s
        INNER JOIN special_lecture_enrollment sle ON s.schedule_id = sle.special_lecture_id
        WHERE sle.student_id = :studentId
          AND s.start_time < :to
          AND s.end_time > :from
          AND s.deleted_date IS NULL
        ORDER BY start_time
        """, nativeQuery = true)
    List<ScheduleRow> findStudentSchedules(
            @Param("studentId") Long studentId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // 학부모는 상담 참석자로 이어진다 — consultation_attendee.member_id가 학부모 식별자다.
    // (이 FK 컬럼명은 V2 마이그레이션에서 consultaition_id 오타를 고친 그 컬럼이다)
    @Query(value = """
        SELECT s.schedule_id, s.type, s.title, s.start_time, s.end_time, s.memo
        FROM schedule s
        INNER JOIN consultation c ON s.schedule_id = c.schedule_id
        INNER JOIN consultation_attendee ca ON ca.consultation_id = c.schedule_id
        WHERE ca.member_id = :parentId
          AND s.start_time < :to
          AND s.end_time > :from
          AND s.deleted_date IS NULL
        ORDER BY s.start_time
        """, nativeQuery = true)
    List<ScheduleRow> findParentSchedules(
            @Param("parentId") Long parentId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
