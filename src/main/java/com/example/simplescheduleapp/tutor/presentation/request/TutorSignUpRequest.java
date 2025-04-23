package com.example.simplescheduleapp.tutor.presentation.request;

import com.example.simplescheduleapp.tutor.application.command.TutorSignUpCommand;

public record TutorSignUpRequest(
        String username,
        String password,
        String name,
        int age,
        String phoneNumber,
        int careerPeriod
) {

    public TutorSignUpCommand toCommand() {
        return new TutorSignUpCommand(username, password, name, age, phoneNumber, careerPeriod);
    }
}
