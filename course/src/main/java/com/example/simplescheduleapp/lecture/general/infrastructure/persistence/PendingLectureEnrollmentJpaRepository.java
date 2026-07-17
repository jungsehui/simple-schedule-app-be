package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface PendingLectureEnrollmentJpaRepository extends JpaRepository<PendingLectureEnrollmentEntity, Long> {

    boolean existsByLectureIdAndStudentId(Long lectureId, Long studentId);

    // 파라미터명은 메서드명의 파생 순서(lecture_id, student_id)와 일치해야 한다.
    // (구버전은 (studentId, lectureId)로 뒤바뀐 이름이라 오해를 유발했다 — 바인딩은 위치 기반이라 동작은 정상이었음)
    Optional<PendingLectureEnrollmentEntity> findByLectureIdAndStudentId(Long lectureId, Long studentId);
}
