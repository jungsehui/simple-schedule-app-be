package com.example.simplescheduleapp.lecture.special.presentation.response;

import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;

import java.time.LocalDateTime;

public record SpecialLectureCreateResponse(
        Long specialLectureId,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String memo,
        int capacity
) {

    public static SpecialLectureCreateResponse from(SpecialLecture specialLecture) {
        return new SpecialLectureCreateResponse(
                specialLecture.getId(),
                specialLecture.getTitle(),
                specialLecture.getStartTime(),
                specialLecture.getEndTime(),
                specialLecture.getMemo(),
                specialLecture.getCapacity()
        );
    }
}
