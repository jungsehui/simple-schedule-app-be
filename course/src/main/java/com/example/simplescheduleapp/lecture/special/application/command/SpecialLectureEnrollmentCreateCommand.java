package com.example.simplescheduleapp.lecture.special.application.command;

public record SpecialLectureEnrollmentCreateCommand(
        Long studentId,
        Long specialLectureId
) {

    public static SpecialLectureEnrollmentCreateCommand of(Long studentId, Long specialLectureId) {
        return new SpecialLectureEnrollmentCreateCommand(studentId, specialLectureId);
    }
}
