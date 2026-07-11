package com.example.simplescheduleapp.lecture.general.application.command;

public record LectureEnrollmentCancelCommand(
        Long studentId,
        Long lectureId
) {

    public static LectureEnrollmentCancelCommand of(Long studentId, Long lectureId) {
        return new LectureEnrollmentCancelCommand(studentId, lectureId);
    }
}
