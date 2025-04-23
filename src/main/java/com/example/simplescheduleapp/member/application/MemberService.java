package com.example.simplescheduleappback.member.application;

import com.example.simplescheduleappback.common.exception.ApplicationException;
import com.example.simplescheduleappback.member.domain.entity.Member;
import com.example.simplescheduleappback.member.domain.repository.MemberRepository;
import com.example.simplescheduleappback.member.domain.service.MemberRegister;
import com.example.simplescheduleappback.member.exception.MemberExceptionCode;
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
