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

    /**
     * 이 학생의 확정 수강등록 전부.
     *
     * <p>{@code findAllByLectureId}와 달리 {@code Optional}로 감싸지 않는다 — 저 쪽은 "이 강의에
     * 수강생이 없다"를 예외로 번역하는 오래된 계약이지만, "이 학생이 아직 아무것도 신청하지
     * 않았다"는 <b>정상 상태</b>다. 빈 목록이 정답인 곳에 예외를 쓰면 첫 화면이 에러가 된다.
     */
    List<LectureEnrollment> findAllByStudentId(Long studentId);

    default LectureEnrollment getByLectureIdAndStudentId(Long lectureId, Long studentId) {
        return findByLectureIdAndStudentId(lectureId, studentId)
                .orElseThrow(() -> new ApplicationException(LectureEnrollmentExceptionCode.LECTURE_ENROLLMENT_NOT_FOUND));
    }

    default List<LectureEnrollment> getAllByLectureId(Long lectureId) {
        return findAllByLectureId(lectureId)
                .orElseThrow(() -> new ApplicationException(LectureEnrollmentExceptionCode.LECTURE_ENROLLMENT_NOT_FOUND));
    }
}
