package com.example.simplescheduleapp.lecture.application.command;

import java.time.LocalDateTime;

public record LectureUpdateCommand(
        Long tutorId,
        Long lectureId,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo,
        int capacity
) {
}
