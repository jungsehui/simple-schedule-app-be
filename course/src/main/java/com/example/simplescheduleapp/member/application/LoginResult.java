package com.example.simplescheduleapp.member.application;

import com.example.simplescheduleapp.common.auth.Role;

/**
 * 로그인 결과(application 계층 → presentation). 엔티티를 노출하지 않고 토큰 발급에 필요한
 * 식별자와 역할만 전달한다.
 */
public record LoginResult(
        Long memberId,
        Role role
) {
}
