package com.example.simplescheduleapp.lecture.general.presentation.response;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;

import java.util.List;

public record LectureSearchResponse(
        List<LectureResponse> lectureResponses
) {

    public static LectureSearchResponse from(List<Lecture> lectures) {
        List<LectureResponse> lectureResponses = lectures.stream()
                .map(it -> new LectureResponse(
                        it.getId(),
                        it.getTitle(),
                        it.getStartTime(),
                        it.getEndTime(),
                        it.getMemo(),
                        it.getCapacity(),
                        it.getEnrolledCount()
                ))
                .toList();
        return new LectureSearchResponse(lectureResponses);
    }
}
