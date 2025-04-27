package com.example.simplescheduleapp.lecture.application.command;

import com.example.simplescheduleapp.lecture.domain.entity.Lecture;

import java.time.LocalDateTime;

public record LectureCreateCommand(
        Long memberId,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo
) {

    public Lecture toLecture() {
        return new Lecture(title, startTime, endTime, memo);
    }
}
