package com.example.simplescheduleapp.member.application;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.domain.MemberRepository;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public Long login(String username, String password) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new ApplicationException(MemberExceptionCode.INVALID_USERNAME_PASSWORD));
        member.login(password);
        return member.getId();
    }
}
