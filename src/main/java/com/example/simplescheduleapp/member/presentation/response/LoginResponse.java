package com.example.simplescheduleappback.member.presentation.response;

public record LoginResponse(
        Long memberId,
        String accessToken
) {
}
