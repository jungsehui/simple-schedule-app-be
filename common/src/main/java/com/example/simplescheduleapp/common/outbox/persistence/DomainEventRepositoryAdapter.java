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
 * <p>Spring Data/JPA 세부와 도메인↔아웃박스 엔티티 매핑을 여기에 격리한다. 서브타입별 변환은
 * {@link DomainEventPersistenceMapperFactory}가 찾아준 전략에 위임한다.
 */
@Repository
@RequiredArgsConstructor
public class DomainEventRepositoryAdapter implements DomainEventRepository {

    private final DomainEventJpaRepository jpaRepository;
    private final DomainEventPersistenceMapperFactory mapperFactory;

    @Override
    public DomainEvent save(DomainEvent domainEvent) {
        DomainEventEntity entity = mapperFactory.getMapper(domainEvent).toEntity(domainEvent);
        DomainEventEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<DomainEvent> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<DomainEvent> findByUuid(String uuid) {
        return jpaRepository.findByUuid(uuid).map(this::toDomain);
    }

    @Override
    public List<DomainEvent> findByStatus(EventStatus status) {
        return jpaRepository.findByStatus(status).stream().map(this::toDomain).toList();
    }

    @Override
    public List<DomainEvent> findByStatusAndCreatedDateBefore(EventStatus status, LocalDateTime before) {
        return jpaRepository.findByStatusAndCreatedDateBefore(status, before).stream().map(this::toDomain).toList();
    }

    private DomainEvent toDomain(DomainEventEntity entity) {
        return mapperFactory.getMapper(entity).toDomain(entity);
    }
}
