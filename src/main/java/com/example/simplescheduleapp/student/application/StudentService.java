package com.example.simplescheduleappback.student.application;

import com.example.simplescheduleappback.student.application.command.StudentSignUpCommand;
import com.example.simplescheduleappback.student.domain.service.StudentRegister;
import com.example.simplescheduleappback.student.domain.entity.Student;
import com.example.simplescheduleappback.student.domain.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class StudentService {

    private final StudentRegister studentRegister;
    private final StudentRepository studentRepository;

    public Long signUpStudent(StudentSignUpCommand studentSignUpCommand) {
        Student student = studentSignUpCommand.toStudent();
        Student registeredStudent = studentRegister.register(student);
        return registeredStudent.getId();
    }
}
