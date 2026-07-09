package com.example.simplescheduleapp.common.event.mapper;

import com.example.simplescheduleapp.common.event.DomainEvent;
import com.example.simplescheduleapp.common.event.exception.DomainEventExceptionCode;
import com.example.simplescheduleapp.common.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class DomainEventMapperFactory {

    private final List<DomainEventMapper> mappers;

    public DomainEventMapper getMapper(DomainEvent event) {
        return mappers.stream()
                .filter(mapper -> mapper.supports(event))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(DomainEventExceptionCode.DOMAIN_EVENT_NOT_SUPPORTED));
    }
}
