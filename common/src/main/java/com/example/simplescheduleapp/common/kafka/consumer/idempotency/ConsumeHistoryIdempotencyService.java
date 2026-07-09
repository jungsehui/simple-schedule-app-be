package com.example.simplescheduleapp.common.kafka.consumer.idempotency;

import com.example.simplescheduleapp.common.kafka.consumer.KafkaMessageConsumeHistory;
import com.example.simplescheduleapp.common.kafka.consumer.KafkaMessageProcessConsumeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ConsumeHistoryIdempotencyService implements IdempotencyService {

    private final KafkaMessageProcessConsumeHistoryRepository kafkaMessageProcessConsumeHistoryRepository;

    @Override
    public boolean isDuplicated(String uuid) {
        Optional<KafkaMessageConsumeHistory> optionalHistory = kafkaMessageProcessConsumeHistoryRepository.findByUuid(uuid);
        return optionalHistory.isPresent();
    }

    @Override
    public void saveProcessed(String uuid, String topic) {
        KafkaMessageConsumeHistory kafkaMessageConsumeHistory = new KafkaMessageConsumeHistory(uuid, topic);
        kafkaMessageProcessConsumeHistoryRepository.save(kafkaMessageConsumeHistory);
    }
}
