package com.example.simplescheduleapp.common.kafka.deadletter;

import com.example.simplescheduleapp.common.kafka.KafkaDomainEventMessage;
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
        KafkaDomainEventMessage data = (KafkaDomainEventMessage) consumerRecord.value();

        String uuid = data.uuid();
        String topic = consumerRecord.topic();
        Long offset = consumerRecord.offset();

        log.info("Record deadLetter. uuid: {}, topic: {}, offset: {}. e: {}, cause: {}, cause: {}".formatted(
                        uuid, topic, offset, e.getClass(), e.getCause(), e.getCause().getMessage()
                ));

        DeadLetter deadLetter = new DeadLetter(
                uuid,
                e.getCause() == null ? e.getMessage() : e.getCause().getMessage(),
                false
        );

        deadLetterRepository.save(deadLetter);
        log.info("Successfully saved deadLetter. dead letter ID: {}. uuid: {}", deadLetter.getId(), uuid);
    }
}
