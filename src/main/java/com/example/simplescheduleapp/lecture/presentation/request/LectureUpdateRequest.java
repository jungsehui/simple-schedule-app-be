package com.example.simplescheduleapp.lecture.presentation.request;

import com.example.simplescheduleapp.lecture.application.command.LectureUpdateCommand;

import java.time.LocalDateTime;

public record LectureUpdateRequest(
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo,
        int capacity
) {

    public LectureUpdateCommand toCommand(Long tutorId, Long lectureId) {
        return new LectureUpdateCommand(
                tutorId,
                lectureId,
                title,
                startTime,
                endTime,
                memo,
                capacity
        );
    }
}
