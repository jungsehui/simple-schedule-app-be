package com.example.simplescheduleapp.member.domain.service;

import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.domain.MemberSubtypeRepository;
import lombok.RequiredArgsConstructor;

/**
 * 회원 서브타입 등록 도메인 서비스.
 *
 * <p>순수 자바 — Spring Data/예외에 의존하지 않는다. 중복(username/phone) 시의 도메인 예외 번역은
 * {@link MemberSubtypeRepository#save} 계약에 위임한다(어댑터가 담당). (ADR-0002 Stage 2)
 */
@RequiredArgsConstructor
public abstract class MemberRegister<T extends Member> {

    private final MemberSubtypeRepository<T> memberRepository;

    public T register(T member) {
        return memberRepository.save(member);
    }
}
