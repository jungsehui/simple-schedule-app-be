package com.example.simplescheduleapp.consultation.domain.repository;

import com.example.simplescheduleapp.consultation.domain.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
}
