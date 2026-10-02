package com.example.simplescheduleapp.common.kafka.consumer;

import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import com.example.simplescheduleapp.common.kafka.consumer.idempotency.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.stereotype.Component;

/**
 * 리스너가 처리에 성공한 레코드만 처리 기록(kafka_message_consume_history)에 남긴다.
 *
 * <p>{@link KafkaIdempotencyFilter}는 기록을 확인만 하고, 기록은 여기서 남긴다. 처리 전에 기록하면
 * DefaultErrorHandler의 재시도가 필터에서 "이미 처리됨"으로 걸러져, 실패한 메시지가 재시도도
 * dead letter도 없이 사라진다. {@code success}는 리스너가 예외 없이 끝났을 때만 호출된다.
 *
 * <p>필터에서 걸러진 중복 레코드에도 {@code success}가 호출될 수 있으므로, 이미 기록이 있으면 다시 남기지 않는다.
 * 처리와 기록은 한 트랜잭션이 아니다: 기록 저장이 실패하면 재시도로 같은 메시지가 한 번 더 처리될 수 있다
 * (유실보다 중복을 택한 at-least-once).
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaProcessedRecordInterceptor implements RecordInterceptor<String, KafkaLectureEventMessage> {

    private final IdempotencyService idempotencyService;

    @Override
    public ConsumerRecord<String, KafkaLectureEventMessage> intercept(
            ConsumerRecord<String, KafkaLectureEventMessage> consumerRecord,
            Consumer<String, KafkaLectureEventMessage> consumer
    ) {
        return consumerRecord;
    }

    @Override
    public void success(
            ConsumerRecord<String, KafkaLectureEventMessage> consumerRecord,
            Consumer<String, KafkaLectureEventMessage> consumer
    ) {
        KafkaLectureEventMessage data = consumerRecord.value();
        if (data == null || data.uuid() == null) {
            return;
        }
        if (idempotencyService.isDuplicated(data.uuid())) {
            return;
        }
        idempotencyService.saveProcessed(data.uuid(), consumerRecord.topic());
        log.info("Saved processed record. uuid: {}, topic: {}", data.uuid(), consumerRecord.topic());
    }
}
