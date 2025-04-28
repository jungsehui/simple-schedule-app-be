package com.example.simplescheduleapp.lecture.presentation.response;

import com.example.simplescheduleapp.lecture.domain.entity.Lecture;

import java.util.List;

public record LectureSearchResponse(
        List<Lecture> lectures
) {

    public static LectureSearchResponse from(List<Lecture> lectures) {
        return new LectureSearchResponse(lectures);
    }
}
