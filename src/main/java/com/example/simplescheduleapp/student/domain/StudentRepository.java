package com.example.simplescheduleapp.student.domain;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.member.exception.MemberExceptionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    default Student getById(Long id) {
        return findById(id).orElseThrow(() -> new ApplicationException(MemberExceptionCode.STUDENT_NOT_FOUND));
    }

    default Student getByPhoneNumber(String phoneNumber) {
        return findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ApplicationException(MemberExceptionCode.STUDENT_NOT_FOUND));
    }

    Optional<Student> findByPhoneNumber(String phoneNumber);
}
