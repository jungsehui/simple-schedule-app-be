package com.example.simplescheduleapp.lecture.general.application.command;

public record PendingAcceptCommand(
        Long memberId, // Phase 3a: 토큰 식별자(무토큰 레거시 요청은 null) — 소유권 검증용
        Long pendingId
) {
}
