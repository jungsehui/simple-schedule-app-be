package com.example.simplescheduleapp.lecture.general.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.general.exception.LectureEnrollmentExceptionCode;

import java.util.List;
import java.util.Optional;

public interface LectureEnrollmentRepository {

    LectureEnrollment save(LectureEnrollment e);

    void delete(LectureEnrollment e);

    Optional<List<LectureEnrollment>> findAllByLectureId(Long lectureId);

    Optional<LectureEnrollment> findByLectureIdAndStudentId(Long lectureId, Long studentId);

    default LectureEnrollment getByLectureIdAndStudentId(Long lectureId, Long studentId) {
        return findByLectureIdAndStudentId(lectureId, studentId)
                .orElseThrow(() -> new ApplicationException(LectureEnrollmentExceptionCode.LECTURE_ENROLLMENT_NOT_FOUND));
    }

    default List<LectureEnrollment> getAllByLectureId(Long lectureId) {
        return findAllByLectureId(lectureId)
                .orElseThrow(() -> new ApplicationException(LectureEnrollmentExceptionCode.LECTURE_ENROLLMENT_NOT_FOUND));
    }
}
