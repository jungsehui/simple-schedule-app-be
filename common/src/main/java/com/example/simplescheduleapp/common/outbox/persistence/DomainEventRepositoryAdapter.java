package com.example.simplescheduleapp.common.outbox.persistence;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.DomainEventRepository;
import com.example.simplescheduleapp.common.event.EventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * {@code DomainEventRepository} 포트의 JPA 어댑터 (ADR-0004).
 *
 * <p>Spring Data 세부를 이벤트 추상화(common.event) 밖으로 격리한다. {@code DomainEvent}는 아직
 * JPA 엔티티이므로 매핑 없이 위임한다 — 도메인/영속 모델 3분리(ADR-0002 §4)는 후속 작업.
 */
@Repository
@RequiredArgsConstructor
public class DomainEventRepositoryAdapter implements DomainEventRepository {

    private final DomainEventJpaRepository jpaRepository;

    @Override
    public DomainEvent save(DomainEvent domainEvent) {
        return jpaRepository.save(domainEvent);
    }

    @Override
    public Optional<DomainEvent> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<DomainEvent> findByUuid(String uuid) {
        return jpaRepository.findByUuid(uuid);
    }

    @Override
    public List<DomainEvent> findByStatus(EventStatus status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public List<DomainEvent> findByStatusAndCreatedDateBefore(EventStatus status, LocalDateTime before) {
        return jpaRepository.findByStatusAndCreatedDateBefore(status, before);
    }
}
