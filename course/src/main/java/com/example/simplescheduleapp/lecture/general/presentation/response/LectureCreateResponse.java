package com.example.simplescheduleapp.lecture.general.presentation.response;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;

import java.time.LocalDateTime;

public record LectureCreateResponse(
        Long lectureId,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo,
        int capacity
) {

    public static LectureCreateResponse from(Lecture lecture) {
        return new LectureCreateResponse(
                lecture.getId(),
                lecture.getTitle(),
                lecture.getStartTime(),
                lecture.getEndTime(),
                lecture.getMemo(),
                lecture.getCapacity()
        );
    }
}
