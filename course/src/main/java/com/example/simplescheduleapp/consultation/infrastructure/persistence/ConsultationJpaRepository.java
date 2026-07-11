package com.example.simplescheduleapp.consultation.infrastructure.persistence;

import com.example.simplescheduleapp.consultation.domain.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA 리포지토리 — {@link ConsultationRepositoryAdapter}가 이 인터페이스로
 * {@code ConsultationRepository} 포트를 구현한다. Spring Data는 이 infrastructure 계층에만 존재한다.
 */
public interface ConsultationJpaRepository extends JpaRepository<Consultation, Long> {
}
