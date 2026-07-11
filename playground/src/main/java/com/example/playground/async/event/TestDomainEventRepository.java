package com.example.playground.async.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestDomainEventRepository extends JpaRepository<TestDomainEvent, Long> {

    default TestDomainEvent getById(Long id) {
        return findById(id).orElseThrow();
    }

    default TestDomainEvent getByUuid(String uuid) {
        return findByUuid(uuid).orElseThrow();
    }

    Optional<TestDomainEvent> findByUuid(String uuid);
}
