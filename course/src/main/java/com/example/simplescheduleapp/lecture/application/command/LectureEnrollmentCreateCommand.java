package com.example.simplescheduleapp.lecture.application.command;

public record LectureEnrollmentCreateCommand(
        Long studentId,
        Long lectureId
) {

    public static LectureEnrollmentCreateCommand of(Long studentId, Long lectureId) {
        return new LectureEnrollmentCreateCommand(studentId, lectureId);
    }
}
