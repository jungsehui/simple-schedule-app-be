package com.example.simplescheduleapp.common.kafka.deadletter;

import com.example.simplescheduleapp.common.kafka.KafkaLectureEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class DeadLetterRecorder implements ConsumerRecordRecoverer {

    private final DeadLetterRepository deadLetterRepository;

    @Override
    public void accept(ConsumerRecord<?, ?> consumerRecord, Exception e) {
        KafkaLectureEventMessage data = (KafkaLectureEventMessage) consumerRecord.value();

        String uuid = data.uuid();
        String topic = consumerRecord.topic();
        Long offset = consumerRecord.offset();

        // 원인 예외의 메시지는 cause가 null일 수 있으므로 아래 저장부와 동일하게 널 가드한다.
        String failReason = e.getCause() == null ? e.getMessage() : e.getCause().getMessage();

        log.info("Record deadLetter. uuid: {}, topic: {}, offset: {}. e: {}, cause: {}, message: {}",
                        uuid, topic, offset, e.getClass(), e.getCause(), failReason);

        DeadLetter deadLetter = new DeadLetter(
                uuid,
                failReason,
                false
        );

        deadLetterRepository.save(deadLetter);
        log.info("Successfully saved deadLetter. dead letter ID: {}. uuid: {}", deadLetter.getId(), uuid);
    }
}
