package com.example.simplescheduleappback.parent.presentation.request;

import com.example.simplescheduleappback.parent.application.command.ParentSignUpCommand;

public record ParentSignUpRequest(
        String username,
        String password,
        String name,
        int age,
        String phoneNumber,
        int childrenNumber
) {

    public ParentSignUpCommand toCommand() {
        return new ParentSignUpCommand(username, password, name, age, phoneNumber, childrenNumber);
    }
}
