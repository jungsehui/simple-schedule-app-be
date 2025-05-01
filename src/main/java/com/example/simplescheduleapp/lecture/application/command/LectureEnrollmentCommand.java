package com.example.simplescheduleapp.lecture.application.command;

public record LectureEnrollmentCommand(
        Long studentId,
        Long lectureId
) {

    public static LectureEnrollmentCommand of(Long studentId, Long lectureId) {
        return new LectureEnrollmentCommand(studentId, lectureId);
    }
}
