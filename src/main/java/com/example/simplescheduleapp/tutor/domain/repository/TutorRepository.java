package com.example.simplescheduleapp.tutor.domain.repository;

import com.example.simplescheduleapp.tutor.domain.entity.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, Long> {

    Optional<Tutor> findById(Long id);
}
