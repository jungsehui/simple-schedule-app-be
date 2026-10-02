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

    // true 반환 시 처리하지 않음.
    // 처리 기록은 여기서 남기지 않는다. 처리 전에 기록하면 DefaultErrorHandler의 재시도가 이 필터에서
    // "이미 처리됨"으로 걸러져, 실패한 메시지가 재시도도 dead letter도 없이 사라진다.
    // 기록은 리스너가 처리에 성공한 뒤 남긴다 (IdempotencyService.saveProcessed).
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

        return false; // false면 통과 (로직 수행)
    }
}
