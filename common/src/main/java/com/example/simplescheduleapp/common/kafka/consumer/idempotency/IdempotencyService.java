package com.example.simplescheduleapp.common.kafka.consumer.idempotency;

import com.example.simplescheduleapp.common.kafka.KafkaDomainEventMessage;

public interface IdempotencyService {

    boolean isDuplicated(KafkaDomainEventMessage data);

    void saveProcessed(KafkaDomainEventMessage data, String topic);
}
