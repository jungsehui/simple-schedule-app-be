package com.example.simplescheduleapp.lecture.general.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.general.exception.LectureEnrollmentExceptionCode;

import java.util.Optional;

public interface PendingLectureEnrollmentRepository {

    PendingLectureEnrollment save(PendingLectureEnrollment e);

    void delete(PendingLectureEnrollment e);

    boolean existsByLectureIdAndStudentId(Long lectureId, Long studentId);

    Optional<PendingLectureEnrollment> findById(Long id);

    // 파라미터명은 메서드명 순서(lectureId, studentId)와 일치시킨다.
    // (구버전은 이름이 뒤바뀌어 있었다 — 호출은 위치 기반이라 동작은 정상이었으나 오해를 유발)
    Optional<PendingLectureEnrollment> findByLectureIdAndStudentId(Long lectureId, Long studentId);

    default PendingLectureEnrollment getById(Long pendingId) {
        return findById(pendingId)
                .orElseThrow(() -> new ApplicationException(LectureEnrollmentExceptionCode.PENDING_LECTURE_ENROLLMENT_NOT_FOUND));
    }

    default PendingLectureEnrollment getByLectureIdAndStudentId(Long lectureId, Long studentId) {
        return findByLectureIdAndStudentId(lectureId, studentId)
                .orElseThrow(() -> new ApplicationException(LectureEnrollmentExceptionCode.PENDING_LECTURE_ENROLLMENT_NOT_FOUND));
    }
}
