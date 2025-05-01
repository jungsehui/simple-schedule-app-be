package com.example.simplescheduleapp.student.application;

import com.example.simplescheduleapp.student.application.command.StudentSignUpCommand;
import com.example.simplescheduleapp.student.domain.service.StudentRegister;
import com.example.simplescheduleapp.student.domain.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class StudentService {

    private final StudentRegister studentRegister;

    public Long signUpStudent(StudentSignUpCommand studentSignUpCommand) {
        Student student = studentSignUpCommand.toStudent();
        Student registeredStudent = studentRegister.register(student);
        return registeredStudent.getId();
    }
}
