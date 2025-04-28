package com.example.simplescheduleapp.lecture.application.command;

import com.example.simplescheduleapp.lecture.domain.entity.Lecture;

import java.time.LocalDateTime;

public record LectureUpdateCommand(
        Long lectureId,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo
) {

    public Lecture toLecture() {
        return new Lecture(title, startTime, endTime, memo);
    }
}
