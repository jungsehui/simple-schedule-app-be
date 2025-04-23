package com.example.simplescheduleapp.parent.presentation.request;

import com.example.simplescheduleapp.parent.application.command.ParentSignUpCommand;

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
