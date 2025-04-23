package com.example.simplescheduleappback.student.domain.service;

import com.example.simplescheduleappback.common.exception.ApplicationException;
import com.example.simplescheduleappback.member.domain.service.MemberRegister;
import com.example.simplescheduleappback.student.domain.entity.Student;
import com.example.simplescheduleappback.student.domain.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import static com.example.simplescheduleappback.member.exception.MemberExceptionCode.DUPLICATED_USERNAME_PHONE;

@RequiredArgsConstructor
@Component
public class StudentRegister implements MemberRegister<Student> {

    private final StudentRepository studentRepository;

    @Override
    public Student register(Student student) {
        try {
            return studentRepository.save(student);
        } catch (DataIntegrityViolationException e) {
            throw new ApplicationException(DUPLICATED_USERNAME_PHONE);
        }
    }
}
