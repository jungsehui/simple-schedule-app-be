package com.example.simplescheduleapp.lecture.application.command;

import java.time.LocalDateTime;

public record LectureCreateCommand(
        Long memberId,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo,
        int capacity
) {
}
