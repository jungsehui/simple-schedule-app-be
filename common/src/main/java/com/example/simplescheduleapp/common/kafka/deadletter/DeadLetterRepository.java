package com.example.simplescheduleapp.common.kafka.deadletter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeadLetterRepository extends JpaRepository<DeadLetter, Long> {

    Optional<DeadLetter> findByUuid(String uuid);
}
