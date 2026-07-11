package com.example.simplescheduleapp.parent.domain;

import com.example.simplescheduleapp.member.domain.MemberSubtypeRepository;

/**
 * 학부모 영속성 포트 (구현: infrastructure JPA 어댑터). 순수 자바 — Spring Data 미참조. (ADR-0002 Stage 2)
 */
public interface ParentRepository extends MemberSubtypeRepository<Parent> {
}
