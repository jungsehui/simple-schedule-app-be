package com.example.simplescheduleappback.student.domain.repository;

import com.example.simplescheduleappback.student.domain.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
}
