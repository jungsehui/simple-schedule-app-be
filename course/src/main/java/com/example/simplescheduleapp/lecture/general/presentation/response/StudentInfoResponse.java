package com.example.simplescheduleapp.lecture.general.presentation.response;

import com.example.simplescheduleapp.lecture.general.domain.LectureEnrollment;
import com.example.simplescheduleapp.student.domain.Student;

import java.util.List;

public record StudentInfoResponse(
        String name,
        String phoneNumber
) {

    public static List<StudentInfoResponse> from(List<LectureEnrollment> lectureEnrollments) {
        return lectureEnrollments.stream()
                .map(it -> {
                    Student student = it.getStudent();
                    return new StudentInfoResponse(student.getName(), student.getPhoneNumber());
                })
                .toList();
    }
}
