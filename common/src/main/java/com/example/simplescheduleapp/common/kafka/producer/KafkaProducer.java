package com.example.simplescheduleapp.common.kafka.producer;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.InternalServerExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaProducer<T> {

    private final KafkaTemplate<String, T> kafkaTemplate;

    public void produce(String topic, T message) {
        try {
            log.info("Try to produce topic using kafkaTemplate. topic: {}, domain: {}", topic, message);
            kafkaTemplate.send(topic, message).get();
            log.info("Successfully produced topic using kafkaTemplate. topic: {}, domain: {}", topic, message);
        } catch (Exception e) {
            log.error("Unexpected exception while send topic using kafkaTemplate. topic: {}, domain: {}",
                    topic, message, e);
            throw new ApplicationException(InternalServerExceptionCode.UNKNOWN_EXCEPTION);
        }
    }
}
