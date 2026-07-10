package com.example.simplescheduleapp.tutor.presentation.request;

import com.example.simplescheduleapp.lecture.general.application.command.PendingRejectCommand;

public record PendingRejectRequest(
        Long pendingId
) {

    public PendingRejectCommand toCommand(Long memberId) {
        return new PendingRejectCommand(memberId, pendingId);
    }
}
