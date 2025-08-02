package com.example.simplescheduleapp.common.kafka.producer;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.example.simplescheduleapp.common.exception.InternalServerExceptionCode.UNKNOWN_EXCEPTION;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaProducer<T> {

    private final KafkaTemplate<String, T> kafkaTemplate;

    public void produce(String topic, T message) {
        try {
            log.info("Try to produce topic using kafkaTemplate. topic: {}, message: {}", topic, message);
            kafkaTemplate.send(topic, message).get();
            log.info("Successfully produced topic using kafkaTemplate. topic: {}, message: {}", topic, message);
        } catch (Exception e) {
            log.error("Unexpected exception while send topic using kafkaTemplate. topic: {}, message: {}",
                    topic, message, e);
            throw new ApplicationException(UNKNOWN_EXCEPTION);
        }
    }
}
