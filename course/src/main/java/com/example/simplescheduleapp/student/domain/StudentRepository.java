package com.example.simplescheduleapp.student.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.domain.MemberSubtypeRepository;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;

/**
 * 학생 영속성 포트 (구현: infrastructure JPA 어댑터). 순수 자바 — Spring Data 미참조. (ADR-0002 Stage 2)
 */
public interface StudentRepository extends MemberSubtypeRepository<Student> {

    default Student getById(Long id) {
        return findById(id).orElseThrow(() -> new ApplicationException(MemberExceptionCode.STUDENT_NOT_FOUND));
    }
}
