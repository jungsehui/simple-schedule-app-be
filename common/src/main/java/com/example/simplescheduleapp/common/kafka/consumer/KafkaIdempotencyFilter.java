package com.example.simplescheduleapp.common.kafka.consumer;

import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.consumer.idempotency.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaIdempotencyFilter implements RecordFilterStrategy<String, KafkaLectureEventMessage> {

    private final IdempotencyService idempotencyService;

    // true 반환 시 처리하지 않음
    @Override
    public boolean filter(ConsumerRecord<String, KafkaLectureEventMessage> consumerRecord) {
        KafkaLectureEventMessage data = consumerRecord.value();

        String uuid = data.uuid(); // 메시지에 포함된 UUID 사용
        String topic = consumerRecord.topic();
        long offset = consumerRecord.offset();

        log.info("Try to filter already processed event record. uuid: {}, topic: {}, offset: {}", uuid, topic, offset);

        // 중복 consume 인 경우, 처리하지 않음
        if (idempotencyService.isDuplicated(uuid)) {
            log.info("Skip duplicated record. uuid: {}, topic: {}", uuid, topic);
            return true; // true면 필터링 (로직 수행 안 함)
        }

        // 최초 consume 인 경우, DB 저장 후 처리
        idempotencyService.saveProcessed(uuid, topic);
        return false; // false면 통과 (로직 수행)
    }
}
