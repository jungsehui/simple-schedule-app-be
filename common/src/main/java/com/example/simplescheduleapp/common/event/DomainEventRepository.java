package com.example.simplescheduleapp.common.event;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.simplescheduleapp.common.event.exception.DomainEventExceptionCode.DOMAIN_EVENT_NOT_FOUND;

@Repository
public interface DomainEventRepository extends JpaRepository<DomainEvent, Long> {

    default DomainEvent getById(Long id) {
        return findById(id).orElseThrow(() -> new ApplicationException(DOMAIN_EVENT_NOT_FOUND));
    }

    default DomainEvent getByUuid(String uuid) {
        return findByUuid(uuid).orElseThrow(() -> new ApplicationException(DOMAIN_EVENT_NOT_FOUND));
    }

    Optional<DomainEvent> findByUuid(String uuid);

    List<DomainEvent> findByStatusAndCreatedDateBefore(EventStatus status, LocalDateTime before);

    List<DomainEvent> findByStatus(EventStatus status);
}
