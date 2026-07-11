package com.example.simplescheduleapp.lecture.general.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.general.exception.LectureEnrollmentExceptionCode;

import java.util.Optional;

public interface PendingLectureEnrollmentRepository {

    PendingLectureEnrollment save(PendingLectureEnrollment e);

    void delete(PendingLectureEnrollment e);

    boolean existsByLectureIdAndStudentId(Long lectureId, Long studentId);

    Optional<PendingLectureEnrollment> findById(Long id);

    Optional<PendingLectureEnrollment> findByLectureIdAndStudentId(Long studentId, Long lectureId);

    default PendingLectureEnrollment getById(Long pendingId) {
        return findById(pendingId)
                .orElseThrow(() -> new ApplicationException(LectureEnrollmentExceptionCode.PENDING_LECTURE_ENROLLMENT_NOT_FOUND));
    }

    default PendingLectureEnrollment getByLectureIdAndStudentId(Long lectureId, Long studentId) {
        return findByLectureIdAndStudentId(lectureId, studentId)
                .orElseThrow(() -> new ApplicationException(LectureEnrollmentExceptionCode.PENDING_LECTURE_ENROLLMENT_NOT_FOUND));
    }
}
