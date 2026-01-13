package com.example.simplescheduleapp.lecture.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.exception.LectureEnrollmentExceptionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LectureEnrollmentRepository extends JpaRepository<LectureEnrollment, Long> {

    default LectureEnrollment getByLectureIdAndStudentId(Long lectureId, Long studentId) {
        return findByLectureIdAndStudentId(lectureId, studentId)
                .orElseThrow(() -> new ApplicationException(LectureEnrollmentExceptionCode.LECTURE_ENROLLMENT_NOT_FOUND));
    }

    default List<LectureEnrollment> getAllByLectureId(Long lectureId) {
        return findAllByLectureId(lectureId)
                .orElseThrow(() -> new ApplicationException(LectureEnrollmentExceptionCode.LECTURE_ENROLLMENT_NOT_FOUND));
    }

    Optional<List<LectureEnrollment>> findAllByLectureId(Long lectureId);

    Optional<LectureEnrollment> findByLectureIdAndStudentId(Long lectureId, Long studentId);
}
