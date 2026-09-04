package com.example.simplescheduleapp.lecture.general.presentation.response;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;

import java.util.List;

public record TutorLectureGetResponse(
        List<LectureResponse> lectureResponses
) {

    public static TutorLectureGetResponse from(List<Lecture> tutorLectures) {
        List<LectureResponse> lectureResponses = tutorLectures.stream()
                .map(LectureResponse::from)
                .toList();
        return new TutorLectureGetResponse(lectureResponses);
    }
}
