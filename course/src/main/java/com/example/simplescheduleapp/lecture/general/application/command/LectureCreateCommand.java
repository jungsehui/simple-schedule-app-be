package com.example.simplescheduleapp.lecture.general.application.command;

import com.example.simplescheduleapp.schedule.domain.Schedule;

import java.time.LocalDateTime;

public record LectureCreateCommand(
        Long memberId,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo,
        int capacity
) {

    public Schedule toSchedule() {
        return new Schedule(title, startTime, endTime, memo);
    }
}
