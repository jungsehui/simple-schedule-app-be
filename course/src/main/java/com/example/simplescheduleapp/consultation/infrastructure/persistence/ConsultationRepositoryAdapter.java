package com.example.simplescheduleapp.consultation.infrastructure.persistence;

import com.example.simplescheduleapp.consultation.domain.Consultation;
import com.example.simplescheduleapp.consultation.domain.ConsultationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@code ConsultationRepository} 포트의 JPA 어댑터. 도메인은 포트에만 의존하고,
 * Spring Data 세부는 여기에 격리된다. (ADR-0002 Stage 2)
 */
@Repository
@RequiredArgsConstructor
public class ConsultationRepositoryAdapter implements ConsultationRepository {

    private final ConsultationJpaRepository jpaRepository;

    @Override
    public Consultation save(Consultation consultation) {
        return jpaRepository.save(consultation);
    }

    @Override
    public Optional<Consultation> findById(Long id) {
        return jpaRepository.findById(id);
    }
}
