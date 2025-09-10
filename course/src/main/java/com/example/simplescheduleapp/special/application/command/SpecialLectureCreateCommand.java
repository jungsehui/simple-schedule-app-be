package com.example.simplescheduleapp.special.application.command;

import java.time.LocalDateTime;

public record SpecialLectureCreateCommand(
        Long memberId,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo,
        int capacity
) {
}
