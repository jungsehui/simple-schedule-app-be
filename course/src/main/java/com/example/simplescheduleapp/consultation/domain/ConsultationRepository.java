package com.example.simplescheduleapp.consultation.domain;

import java.util.Optional;

/**
 * 아웃바운드 포트: 상담 영속성 (구현: infrastructure의 JPA 어댑터).
 *
 * <p>순수 자바 인터페이스 — Spring Data/JPA가 도메인에 침투하지 않는다. (ADR-0002 Stage 2)
 */
public interface ConsultationRepository {

    Consultation save(Consultation consultation);

    Optional<Consultation> findById(Long id);
}
