package com.example.simplescheduleapp.lecture.presentation.response;

import java.time.LocalDateTime;

public record LectureResponse(
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo,
        int capacity,
        int enrolledCount
) {
}
