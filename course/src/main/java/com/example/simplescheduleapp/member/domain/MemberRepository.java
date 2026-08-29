package com.example.simplescheduleapp.member.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;

import java.util.Optional;

/**
 * 아웃바운드 포트: 회원 영속성 (구현: infrastructure의 JPA 어댑터).
 *
 * <p>순수 자바 인터페이스 — Spring Data/JPA가 도메인에 침투하지 않는다. 조회 실패 시
 * 도메인 예외를 던지는 {@code getXxx} 규약도 여기(도메인)에 위치한다. (ADR-0002 Stage 2)
 */
public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long id);

    Optional<Member> findByUsername(String username);

    default Member getById(Long id) {
        return findById(id).orElseThrow(() -> new ApplicationException(MemberExceptionCode.MEMBER_NOT_FOUND));
    }
}
