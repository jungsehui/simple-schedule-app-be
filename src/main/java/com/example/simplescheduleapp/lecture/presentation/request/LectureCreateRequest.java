package com.example.simplescheduleapp.lecture.presentation.request;

import com.example.simplescheduleapp.lecture.application.command.LectureCreateCommand;

import java.time.LocalDateTime;

public record LectureCreateRequest(
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo
) {

    public LectureCreateCommand toCommand(Long memberId) {
        return new LectureCreateCommand(memberId, title, startTime, endTime, memo);
    }
}
