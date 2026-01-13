package com.example.simplescheduleapp.lecture.general.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.general.exception.LectureEnrollmentExceptionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PendingLectureEnrollmentRepository extends JpaRepository<PendingLectureEnrollment, Long> {

    default PendingLectureEnrollment getById(Long pendingId) {
        return findById(pendingId)
                .orElseThrow(() -> new ApplicationException(LectureEnrollmentExceptionCode.PENDING_LECTURE_ENROLLMENT_NOT_FOUND));
    }

    default PendingLectureEnrollment getByLectureIdAndStudentId(Long lectureId, Long studentId) {
        return findByLectureIdAndStudentId(lectureId, studentId)
                .orElseThrow(() -> new ApplicationException(LectureEnrollmentExceptionCode.PENDING_LECTURE_ENROLLMENT_NOT_FOUND));
    }

    boolean existsByLectureIdAndStudentId(Long lectureId, Long studentId);

    Optional<PendingLectureEnrollment> findByLectureIdAndStudentId(Long studentId, Long lectureId);
}
