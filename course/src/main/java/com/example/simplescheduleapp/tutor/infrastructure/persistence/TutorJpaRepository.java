package com.example.simplescheduleapp.tutor.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TutorJpaRepository extends JpaRepository<TutorEntity, Long> {
}
