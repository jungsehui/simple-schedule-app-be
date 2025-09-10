package com.example.simplescheduleapp.special.presentation.request;

import com.example.simplescheduleapp.special.application.command.SpecialLectureCreateCommand;

import java.time.LocalDateTime;

public record SpecialLectureCreateRequest(
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo,
        int capacity
) {

    public SpecialLectureCreateCommand toCommand(Long tutorId) {
        return new SpecialLectureCreateCommand(tutorId, title, startTime, endTime, memo, capacity);
    }
}
