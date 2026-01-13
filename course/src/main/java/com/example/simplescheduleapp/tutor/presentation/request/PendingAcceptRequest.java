package com.example.simplescheduleapp.tutor.presentation.request;

import com.example.simplescheduleapp.lecture.general.application.command.PendingAcceptCommand;

public record PendingAcceptRequest(
        Long pendingId
) {

    public PendingAcceptCommand toCommand() {
        return new PendingAcceptCommand(pendingId);
    }
}
