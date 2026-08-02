package com.example.simplescheduleapp.common.outbox.persistence;

import com.example.simplescheduleapp.common.event.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA 리포지토리 — {@link DomainEventRepositoryAdapter}가 이 인터페이스로
 * {@code DomainEventRepository} 포트를 구현한다. Spring Data는 이 infrastructure 계층에만 존재한다.
 */
interface DomainEventJpaRepository extends JpaRepository<DomainEventEntity, Long> {

    Optional<DomainEventEntity> findByUuid(String uuid);

    List<DomainEventEntity> findByStatus(EventStatus status);

    List<DomainEventEntity> findByStatusAndCreatedDateBefore(EventStatus status, LocalDateTime before);
}
