package com.example.simplescheduleapp.lecture.general.application.result;

import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import com.example.simplescheduleapp.student.domain.Student;

import java.util.List;

/**
 * 강의 수강생 조회 결과 — 애플리케이션 계층이 관련 애그리게잇을 로드해 조립한다.
 *
 * <p>애그리게잇 간 참조가 ID로 바뀌면서(ADR-0004 Phase A) 프레젠테이션이 도메인 객체 그래프를
 * 순회할 수 없다. 관련 애그리게잇 로드는 애플리케이션 계층의 책임이다.
 */
public record LectureEnrollmentDetail(
        Lecture lecture,
        List<Student> students
) {
}
