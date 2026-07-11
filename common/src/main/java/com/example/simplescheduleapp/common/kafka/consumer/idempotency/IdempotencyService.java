package com.example.simplescheduleapp.common.kafka.consumer.idempotency;

public interface IdempotencyService {

    boolean isDuplicated(String uuid);

    void saveProcessed(String uuid, String topic);
}
