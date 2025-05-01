package com.example.simplescheduleapp.student.application.command;

import com.example.simplescheduleapp.student.domain.Student;

public record StudentSignUpCommand(
        String username,
        String password,
        String name,
        int age,
        String phoneNumber,
        String school
) {

    public Student toStudent() {
        return new Student(username, password, name, age, phoneNumber, school);
    }
}
