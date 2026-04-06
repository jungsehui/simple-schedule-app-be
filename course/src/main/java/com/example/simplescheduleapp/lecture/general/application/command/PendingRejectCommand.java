package com.example.simplescheduleapp.lecture.general.application.command;

public record PendingRejectCommand(
        Long memberId,
        Long pendingId
) {
}
