package com.example.simplescheduleapp.member.application;

import com.example.simplescheduleapp.member.domain.Member;
import com.example.simplescheduleapp.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public LoginResult login(String username, String password) {
        Member member = memberRepository.getByUsername(username);

        // 레거시(무염 SHA-256) 해시로 로그인에 성공하면 도메인이 bcrypt로 승급한다.
        // 승급된 경우에만 저장해 기존 사용자를 끊지 않고 점진 마이그레이션한다. (ADR-0004)
        boolean passwordUpgraded = member.login(password);
        if (passwordUpgraded) {
            memberRepository.save(member);
        }

        return new LoginResult(member.getId(), member.getRole());
    }
}
