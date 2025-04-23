package com.example.simplescheduleappback.student.presentation.request;

import com.example.simplescheduleappback.student.application.command.StudentSignUpCommand;

public record StudentSignUpRequest(
        String username,
        String password,
        String name,
        int age,
        String phoneNumber,
        String school
) {

    public StudentSignUpCommand toCommand() {
        return new StudentSignUpCommand(username, password, name, age, phoneNumber, school);
    }
}
