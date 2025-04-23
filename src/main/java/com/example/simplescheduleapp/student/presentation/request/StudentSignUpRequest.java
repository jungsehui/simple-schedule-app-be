package com.example.simplescheduleapp.student.presentation.request;

import com.example.simplescheduleapp.student.application.command.StudentSignUpCommand;

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
