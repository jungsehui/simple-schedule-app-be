package com.example.simplescheduleapp.lecture.presentation.response;

import com.example.simplescheduleapp.lecture.domain.LectureEnrollment;

import java.time.LocalDateTime;

public record LectureEnrollmentCreateResponse(
        Long lectureEnrollmentId,
        String lectureName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String tutorName,
        int capacity,
        int enrolledCount,
        String studentName
) {

    public static LectureEnrollmentCreateResponse from(LectureEnrollment lectureEnrollment) {
        return new LectureEnrollmentCreateResponse(
                lectureEnrollment.getId(),
                lectureEnrollment.getLecture().getTitle(),
                lectureEnrollment.getLecture().getStartTime(),
                lectureEnrollment.getLecture().getEndTime(),
                lectureEnrollment.getLecture().getTutor().getName(),
                lectureEnrollment.getLecture().getCapacity(),
                lectureEnrollment.getLecture().getEnrolledCount(),
                lectureEnrollment.getStudent().getName()
        );
    }
}
