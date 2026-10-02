package com.example.simplescheduleapp.student.application;

import com.example.simplescheduleapp.student.application.command.StudentSignUpCommand;
import com.example.simplescheduleapp.student.domain.service.StudentRegister;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.member.application.port.out.AccountRegistrationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class StudentService {

    private final StudentRegister studentRegister;
    private final AccountRegistrationPort accountRegistrationPort;

    // 회원과 account를 한 트랜잭션에서 만든다. 어느 쪽이 실패해도 둘 다 롤백된다 (ADR-0006 P1)
    @Transactional
    public Long signUpStudent(StudentSignUpCommand command) {
        Student student = command.toStudent();
        Student registeredStudent = studentRegister.register(student);
        accountRegistrationPort.register(registeredStudent);
        return registeredStudent.getId();
    }
}
