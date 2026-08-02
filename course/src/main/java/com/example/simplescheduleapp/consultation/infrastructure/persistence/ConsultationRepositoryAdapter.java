package com.example.simplescheduleapp.consultation.infrastructure.persistence;

import com.example.simplescheduleapp.consultation.domain.Consultation;
import com.example.simplescheduleapp.consultation.domain.ConsultationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@code ConsultationRepository} 포트의 JPA 어댑터 (ADR-0004).
 * <p>Spring Data/JPA 세부와 도메인↔엔티티 매핑(참석자 합성 포함)을 여기에 격리한다.
 */
@Repository
@RequiredArgsConstructor
public class ConsultationRepositoryAdapter implements ConsultationRepository {

    private final ConsultationJpaRepository jpaRepository;

    @Override
    public Consultation save(Consultation consultation) {
        return ConsultationMapper.toDomain(jpaRepository.save(ConsultationMapper.toEntity(consultation)));
    }

    @Override
    public Optional<Consultation> findById(Long id) {
        return jpaRepository.findById(id).map(ConsultationMapper::toDomain);
    }
}
