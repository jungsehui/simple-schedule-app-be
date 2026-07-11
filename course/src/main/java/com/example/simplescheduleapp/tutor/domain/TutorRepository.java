package com.example.simplescheduleapp.tutor.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.domain.MemberSubtypeRepository;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;

/**
 * 강사 영속성 포트 (구현: infrastructure JPA 어댑터). 순수 자바 — Spring Data 미참조. (ADR-0002 Stage 2)
 */
public interface TutorRepository extends MemberSubtypeRepository<Tutor> {

    default Tutor getById(Long id) {
        return findById(id).orElseThrow(() -> new ApplicationException(MemberExceptionCode.TUTOR_NOT_FOUND));
    }
}
