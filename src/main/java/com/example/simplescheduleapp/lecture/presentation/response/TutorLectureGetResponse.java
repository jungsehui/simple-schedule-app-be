package com.example.simplescheduleapp.lecture.presentation.response;

import com.example.simplescheduleapp.lecture.domain.Lecture;
import com.example.simplescheduleapp.lecture.domain.LectureEnrollment;
import com.example.simplescheduleapp.member.domain.Member;

import java.time.LocalDateTime;
import java.util.List;

public record TutorLectureGetResponse(
        List<LectureResponse> lectureResponses
) {

    public static TutorLectureGetResponse from(List<Lecture> lectures) {
        List<LectureResponse> lectureResponses = lectures.stream()
                .map(it -> new LectureResponse(
                        it.getCreatedAt(),
                        it.getUpdatedAt(),
                        it.getTitle(),
                        it.getStartTime(),
                        it.getEndTime(),
                        it.getMemo(),
                        it.getCapacity(),
                        it.getEnrolledCount(),
                        it.getLectureEnrollments().stream()
                                .map(LectureEnrollment::getStudent)
                                .map(Member::getName)
                                .toList()
                ))
                .toList();

        return new TutorLectureGetResponse(lectureResponses);
    }

    private record LectureResponse(
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String memo,
            int capacity,
            int enrolledCount,
            List<String> studentNames
    ) {
    }
}
