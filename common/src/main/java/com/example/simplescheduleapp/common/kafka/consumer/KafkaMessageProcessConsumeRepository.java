package com.example.simplescheduleapp.common.kafka.consumer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KafkaMessageProcessConsumeRepository extends JpaRepository<KafkaMessageConsumeHistory, Long> {

    Optional<KafkaMessageConsumeHistory> findByUuid(String uuid);
}
