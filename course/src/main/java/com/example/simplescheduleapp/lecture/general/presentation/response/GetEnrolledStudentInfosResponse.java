package com.example.simplescheduleapp.lecture.general.presentation.response;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;

import java.util.List;

public record GetEnrolledStudentInfosResponse(
        String lectureTitle,
        String lectureMemo,
        List<Long> studentIds
) {

    public static GetEnrolledStudentInfosResponse of(Lecture lecture, List<Long> studentIds) {
        return new GetEnrolledStudentInfosResponse(
                lecture.getTitle(),
                lecture.getMemo(),
                studentIds);
    }
}
