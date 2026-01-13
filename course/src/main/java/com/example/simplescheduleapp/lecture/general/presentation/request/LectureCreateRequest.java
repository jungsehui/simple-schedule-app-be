package com.example.simplescheduleapp.lecture.general.presentation.request;

import com.example.simplescheduleapp.lecture.general.application.command.LectureCreateCommand;

import java.time.LocalDateTime;

public record LectureCreateRequest(
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo,
        int capacity
) {

    public LectureCreateCommand toCommand(Long tutorId) {
        return new LectureCreateCommand(tutorId, title, startTime, endTime, memo, capacity);
    }
}
