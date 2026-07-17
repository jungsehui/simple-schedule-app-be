package com.example.simplescheduleapp.student.domain.service;

import com.example.simplescheduleapp.member.domain.service.MemberRegister;
import com.example.simplescheduleapp.student.domain.Student;
import com.example.simplescheduleapp.student.domain.StudentRepository;

public class StudentRegister extends MemberRegister<Student> {

    public StudentRegister(StudentRepository memberRepository) {
        super(memberRepository);
    }
}
