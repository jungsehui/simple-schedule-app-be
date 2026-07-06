package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.general.domain.PendingLectureEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingLectureEnrollmentJpaRepository extends JpaRepository<PendingLectureEnrollment, Long> {

    boolean existsByLectureIdAndStudentId(Long lectureId, Long studentId);

    Optional<PendingLectureEnrollment> findByLectureIdAndStudentId(Long studentId, Long lectureId);
}
