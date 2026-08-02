package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface LectureEnrollmentJpaRepository extends JpaRepository<LectureEnrollmentEntity, Long> {

    List<LectureEnrollmentEntity> findAllByLectureId(Long lectureId);

    Optional<LectureEnrollmentEntity> findByLectureIdAndStudentId(Long lectureId, Long studentId);
}
