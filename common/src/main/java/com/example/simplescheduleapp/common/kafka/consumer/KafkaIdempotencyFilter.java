package com.example.simplescheduleapp.common.kafka.consumer;

import com.example.simplescheduleapp.common.kafka.KafkaDomainEventMessage;
import com.example.simplescheduleapp.common.kafka.consumer.idempotency.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaIdempotencyFilter implements RecordFilterStrategy<String, KafkaDomainEventMessage> {

    private final IdempotencyService idempotencyService;

    // true 반환 시 처리하지 않음
    @Override
    public boolean filter(ConsumerRecord<String, KafkaDomainEventMessage> consumerRecord) {
        KafkaDomainEventMessage data = consumerRecord.value();

        String uuid = data.uuid();
        String topic = consumerRecord.topic();
        long offset = consumerRecord.offset();

        log.info("Try to filter already processed event record. uuid: {}, topic: {}, offset: {}", uuid, topic, offset);

        boolean duplicated = idempotencyService.isDuplicated(data);
        if (duplicated) {
            // 중복 consume 인 경우, 처리하지 않음
            log.info("Skip duplicated record. uuid: {}, topic: {}, offset: {}", uuid, topic, offset);
            return true;
        }

        // 최초 consume 인 경우, DB 저장 후 처리
        log.info("Try to attempt consume first time. uuid: {}, topic: {}, offset: {}", uuid, topic, offset);
        idempotencyService.saveProcessed(data, topic);
        return false;
    }
}
