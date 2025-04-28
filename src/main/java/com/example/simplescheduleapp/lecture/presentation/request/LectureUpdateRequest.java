package com.example.simplescheduleapp.lecture.presentation.request;

import com.example.simplescheduleapp.lecture.application.command.LectureUpdateCommand;

import java.time.LocalDateTime;

public record LectureUpdateRequest(
        Long lectureId,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo
) {

    public LectureUpdateCommand toCommand(Long lectureId) {
        return new LectureUpdateCommand(
                lectureId,
                title,
                startTime,
                endTime,
                memo
        );
    }
}
