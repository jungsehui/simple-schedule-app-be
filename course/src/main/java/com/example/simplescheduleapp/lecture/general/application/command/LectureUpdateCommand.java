package com.example.simplescheduleapp.lecture.application.command;

import com.example.simplescheduleapp.schedule.domain.Schedule;

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

    public Schedule toSchedule() {
        return new Schedule(title, startTime, endTime, memo);
    }
}
