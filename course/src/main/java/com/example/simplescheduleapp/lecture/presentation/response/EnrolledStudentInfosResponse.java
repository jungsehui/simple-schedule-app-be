package com.example.simplescheduleapp.lecture.presentation.response;

import com.example.simplescheduleapp.lecture.domain.Lecture;

import java.util.List;

public record EnrolledStudentInfosResponse(
        String lectureTitle,
        String lectureMemo,
        List<Long> studentIds
) {

    public static EnrolledStudentInfosResponse of(Lecture lecture, List<Long> studentIds) {
        return new EnrolledStudentInfosResponse(
                lecture.getTitle(),
                lecture.getMemo(),
                studentIds);
    }
}
