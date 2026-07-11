package com.example.simplescheduleapp.member.presentation.response;

public record LoginResponse(
        Long memberId,
        String accessToken,
        String role
) {
}
