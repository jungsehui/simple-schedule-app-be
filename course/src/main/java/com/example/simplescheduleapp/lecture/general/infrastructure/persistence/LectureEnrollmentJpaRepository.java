package com.example.simplescheduleapp.lecture.general.infrastructure.persistence;

import com.example.simplescheduleapp.lecture.general.domain.LectureEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LectureEnrollmentJpaRepository extends JpaRepository<LectureEnrollment, Long> {

    Optional<List<LectureEnrollment>> findAllByLectureId(Long lectureId);

    Optional<LectureEnrollment> findByLectureIdAndStudentId(Long lectureId, Long studentId);
}
