package com.example.simplescheduleapp.member.application;

import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public Member login(String username, String password) {
        Member member = memberRepository.getByUsername(username);
        member.login(password);
        return member;
    }
}
