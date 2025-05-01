package com.example.simplescheduleapp.student.domain.repository;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.lecture.domain.entity.Lecture;
import com.example.simplescheduleapp.student.domain.entity.Student;
import com.example.simplescheduleapp.student.domain.exception.StudentExceptionCode;
import com.example.simplescheduleapp.tutor.exception.TutorExceptionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    default Student getById(Long id) {
        return findById(id).orElseThrow(() -> new ApplicationException(StudentExceptionCode.STUDENT_NOT_FOUND));
    }

    default Student getByPhoneNumber(String phoneNumber) {
        return findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ApplicationException(StudentExceptionCode.STUDENT_NOT_FOUND));
    }

    Optional<Student> findByPhoneNumber(String phoneNumber);
}
