package com.example.simplescheduleapp.lecture.general.domain.service;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.general.domain.PendingLectureEnrollment;
import com.example.simplescheduleapp.lecture.general.domain.PendingLectureEnrollmentRepository;
import com.example.simplescheduleapp.lecture.general.exception.LectureExceptionCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PendingLectureEnrollmentService {

    private final PendingLectureEnrollmentRepository pendingLectureEnrollmentRepository;

    public PendingLectureEnrollment create(Long lectureId, Long studentId) {
        if (pendingLectureEnrollmentRepository.existsByLectureIdAndStudentId(lectureId, studentId)) {
            throw new ApplicationException(LectureExceptionCode.ALREADY_REQUESTED);
        }
        return new PendingLectureEnrollment(lectureId, studentId);
    }
}
