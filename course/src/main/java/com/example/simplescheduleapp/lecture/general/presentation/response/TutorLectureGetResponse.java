package com.example.simplescheduleapp.lecture.general.presentation.response;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;

import java.util.List;

public record TutorLectureGetResponse(
        List<LectureResponse> lectureResponses
) {

    public static TutorLectureGetResponse from(List<Lecture> tutorLectures) {
        List<LectureResponse> lectureResponses = tutorLectures.stream()
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
        return new TutorLectureGetResponse(lectureResponses);
    }
}
