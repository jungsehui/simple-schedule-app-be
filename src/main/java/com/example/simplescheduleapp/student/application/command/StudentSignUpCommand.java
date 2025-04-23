package com.example.simplescheduleappback.student.application.command;

import com.example.simplescheduleappback.student.domain.entity.Student;

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
