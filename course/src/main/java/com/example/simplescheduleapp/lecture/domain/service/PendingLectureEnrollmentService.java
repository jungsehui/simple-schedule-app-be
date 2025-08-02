package com.example.simplescheduleapp.lecture.domain.service;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.domain.PendingLectureEnrollment;
import com.example.simplescheduleapp.lecture.domain.PendingLectureEnrollmentRepository;
import com.example.simplescheduleapp.lecture.exception.LectureExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PendingLectureEnrollmentService {

    private final PendingLectureEnrollmentRepository pendingLectureEnrollmentRepository;

    public PendingLectureEnrollment create(Long lectureId, Long studentId) {
        if (pendingLectureEnrollmentRepository.existsByLectureIdAndStudentId(lectureId, studentId)) {
            throw new ApplicationException(LectureExceptionCode.ALREADY_REQUESTED);
        }
        return new PendingLectureEnrollment(lectureId, studentId);
    }
}
