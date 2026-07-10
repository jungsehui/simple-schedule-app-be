package com.example.simplescheduleapp.student.infrastructure.persistence;

import com.example.simplescheduleapp.student.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentJpaRepository extends JpaRepository<Student, Long> {
}
