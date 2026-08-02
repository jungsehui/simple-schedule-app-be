package com.example.simplescheduleapp.common.outbox.persistence;

import com.example.simplescheduleapp.common.event.DomainEvent;

/**
 * 도메인 이벤트 ↔ 아웃박스 엔티티 변환 전략 (ADR-0004).
 *
 * <p>공유 커널(common)은 각 컨텍스트의 이벤트 서브타입(course의 강의 이벤트 등)을 알 수 없으므로,
 * 서브타입을 소유한 쪽이 이 전략을 구현해 등록한다. Kafka 메시지 변환의
 * {@code DomainEventMapper} + {@code DomainEventMapperFactory}와 동일한 패턴이다.
 */
public interface DomainEventPersistenceMapper {

    boolean supports(DomainEvent event);

    boolean supportsEntity(DomainEventEntity entity);

    DomainEventEntity toEntity(DomainEvent event);

    DomainEvent toDomain(DomainEventEntity entity);
}
