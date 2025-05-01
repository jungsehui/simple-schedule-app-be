package com.example.simplescheduleapp.lecture.presentation.response;

import com.example.simplescheduleapp.lecture.domain.LectureEnrollment;

import java.util.List;

public record LectureEnrollmentSearchResponse(
        List<LectureEnrollment> lectureEnrollmentResponses
) {

    public static LectureEnrollmentSearchResponse from(List<LectureEnrollment> lectureEnrollments) {
        return new LectureEnrollmentSearchResponse(lectureEnrollments);
    }

    private List<LectureEnrollmentSearchResponse> toResponse() {
        return null;
    }
}
