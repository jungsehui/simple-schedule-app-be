package com.example.simplescheduleapp.student.domain.service;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.domain.service.MemberRegister;
import com.example.simplescheduleapp.student.domain.entity.Student;
import com.example.simplescheduleapp.student.domain.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import static com.example.simplescheduleapp.member.exception.MemberExceptionCode.DUPLICATED_USERNAME_PHONE;

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
