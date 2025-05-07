package com.example.simplescheduleapp.lecture.presentation.request;

import com.example.simplescheduleapp.lecture.application.command.LectureEnrollmentCreateCommand;

public record LectureEnrollmentCreateRequest(
        Long studentId
) {

    public LectureEnrollmentCreateCommand toCommand(Long lectureId) {
        return new LectureEnrollmentCreateCommand(studentId, lectureId);
    }
}
