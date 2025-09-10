package com.example.simplescheduleapp.special.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecialLectureEnrollmentRepository extends JpaRepository<SpecialLectureEnrollment, Long> {
}
