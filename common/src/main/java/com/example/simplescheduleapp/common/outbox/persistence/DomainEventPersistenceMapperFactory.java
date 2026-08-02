package com.example.simplescheduleapp.common.outbox.persistence;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.InternalServerExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 등록된 {@link DomainEventPersistenceMapper} 중 해당 이벤트/엔티티를 지원하는 것을 찾는다 (ADR-0004).
 * <p>미지의 서브타입은 즉시 실패시킨다 — 조용히 유실되면 아웃박스 전달 보증이 깨지기 때문이다.
 */
@RequiredArgsConstructor
@Component
public class DomainEventPersistenceMapperFactory {

    private final List<DomainEventPersistenceMapper> mappers;

    public DomainEventPersistenceMapper getMapper(DomainEvent event) {
        return mappers.stream()
                .filter(mapper -> mapper.supports(event))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(InternalServerExceptionCode.UNKNOWN_EXCEPTION));
    }

    public DomainEventPersistenceMapper getMapper(DomainEventEntity entity) {
        return mappers.stream()
                .filter(mapper -> mapper.supportsEntity(entity))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(InternalServerExceptionCode.UNKNOWN_EXCEPTION));
    }
}
