package com.example.simplescheduleapp.lecture.general.presentation.response;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;

import java.time.LocalDateTime;

public record LectureUpdateResponse(
        Long lectureId,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo,
        int capacity
) {

    public static LectureUpdateResponse from(Lecture lecture) {
        return new LectureUpdateResponse(
                lecture.getId(),
                lecture.getTitle(),
                lecture.getStartTime(),
                lecture.getEndTime(),
                lecture.getMemo(),
                lecture.getCapacity()
        );
    }
}
