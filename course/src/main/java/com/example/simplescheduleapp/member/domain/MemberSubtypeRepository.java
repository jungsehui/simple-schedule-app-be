package com.example.simplescheduleapp.member.domain;

import java.util.Optional;

/**
 * 회원 서브타입(Student/Tutor/Parent) 영속성 공통 포트.
 *
 * <p>순수 자바 — 각 역할 리포지토리 포트가 이를 확장하고, infrastructure의 JPA 어댑터가 구현한다.
 * {@link #save}는 유니크 제약(username/phone 중복) 위반 시 {@code DUPLICATED_USERNAME_PHONE}
 * 도메인 예외를 던지는 것이 계약이다(어댑터가 Spring 예외를 도메인 예외로 번역). 이 계약 덕분에
 * {@code MemberRegister}는 Spring에 의존하지 않고 순수하게 등록 로직을 표현할 수 있다.
 */
public interface MemberSubtypeRepository<T extends Member> {

    T save(T member);

    Optional<T> findById(Long id);
}
