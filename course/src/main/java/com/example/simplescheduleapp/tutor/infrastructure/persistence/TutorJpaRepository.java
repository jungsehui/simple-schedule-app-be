package com.example.simplescheduleapp.tutor.infrastructure.persistence;

import com.example.simplescheduleapp.tutor.domain.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TutorJpaRepository extends JpaRepository<Tutor, Long> {
}
