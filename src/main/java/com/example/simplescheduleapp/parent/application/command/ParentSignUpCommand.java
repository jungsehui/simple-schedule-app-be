package com.example.simplescheduleapp.parent.application.command;

import com.example.simplescheduleapp.parent.domain.entity.Parent;

public record ParentSignUpCommand(
        String username,
        String password,
        String name,
        int age,
        String phoneNumber,
        int childrenNumber
) {

    public Parent toParent() {
        return new Parent(username, password, name, age, phoneNumber, childrenNumber);
    }
}
