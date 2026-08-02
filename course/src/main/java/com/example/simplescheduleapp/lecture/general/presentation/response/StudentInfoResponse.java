package com.example.simplescheduleapp.lecture.general.presentation.response;

import com.example.simplescheduleapp.student.domain.Student;

import java.util.List;

public record StudentInfoResponse(
        String name,
        String phoneNumber
) {

    /**
     * 애플리케이션 계층이 로드해 전달한 Student 목록으로 응답을 만든다.
     * (ADR-0004 Phase A: 애그리게잇 간 참조가 ID이므로 프레젠테이션은 그래프를 순회하지 않는다.)
     */
    public static List<StudentInfoResponse> from(List<Student> students) {
        return students.stream()
                .map(student -> new StudentInfoResponse(student.getName(), student.getPhoneNumber()))
                .toList();
    }
}
