package com.example.simplescheduleapp.lecture.application.command;

public record PendingLectureEnrollmentCancelCommand(
        Long studentId,
        Long lectureId
) {

    public static PendingLectureEnrollmentCancelCommand of(Long studentId, Long lectureId) {
        return new PendingLectureEnrollmentCancelCommand(studentId, lectureId);
    }
}
