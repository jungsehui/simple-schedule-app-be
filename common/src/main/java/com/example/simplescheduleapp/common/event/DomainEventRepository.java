package com.example.simplescheduleapp.common.event;

import com.example.simplescheduleapp.common.exception.ApplicationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.simplescheduleapp.common.event.exception.DomainEventExceptionCode.DOMAIN_EVENT_NOT_FOUND;

/**
 * 아웃박스(도메인 이벤트) 영속 포트 — 순수 자바 인터페이스 (ADR-0002 Stage 2 / ADR-0004).
 *
 * <p>구현은 {@code common.outbox.persistence}의 JPA 어댑터. 조회 실패 시 도메인 예외를 던지는
 * {@code getXxx} 규약도 여기(추상화)에 위치한다.
 */
public interface DomainEventRepository {

    DomainEvent save(DomainEvent domainEvent);

    Optional<DomainEvent> findById(Long id);

    Optional<DomainEvent> findByUuid(String uuid);

    List<DomainEvent> findByStatus(EventStatus status);

    List<DomainEvent> findByStatusAndCreatedDateBefore(EventStatus status, LocalDateTime before);

    default DomainEvent getById(Long id) {
        return findById(id).orElseThrow(() -> new ApplicationException(DOMAIN_EVENT_NOT_FOUND));
    }

    default DomainEvent getByUuid(String uuid) {
        return findByUuid(uuid).orElseThrow(() -> new ApplicationException(DOMAIN_EVENT_NOT_FOUND));
    }
}
