package com.example.simplescheduleapp.tutor.application.command;

import com.example.simplescheduleapp.tutor.domain.Tutor;

public record TutorSignUpCommand(
        String username,
        String password,
        String name,
        int age,
        String phoneNumber,
        int careerPeriod
) {

    public Tutor toTutor() {
        return new Tutor(username, password, name, age, phoneNumber, careerPeriod);
    }
}
