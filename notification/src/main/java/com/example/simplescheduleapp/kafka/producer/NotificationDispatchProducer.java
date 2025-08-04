package com.example.simplescheduleapp.kafka.producer;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.InternalServerExceptionCode;
import com.example.simplescheduleapp.common.kafka.producer.KafkaProducer;
import com.example.simplescheduleapp.kafka.topic.NotificationDispatchEvent;
import com.example.simplescheduleapp.notification.domain.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationDispatchProducer {

    public static final String NOTIFICATION_DISPATCH_TOPIC = "NOTIFICATION_DISPATCH";

    private final KafkaProducer<NotificationDispatchEvent> kafkaProducer;

    public void produce(NotificationMessage notificationMessage) {
        try {
            NotificationDispatchEvent event = NotificationDispatchEvent.from(notificationMessage);
            log.info("Try to produce notification topic. notificationId: {}", event.notificationId());
            kafkaProducer.produce(NOTIFICATION_DISPATCH_TOPIC, event);
            log.info("Successfully produce notification topic. notificationId: {}", event.notificationId());
        } catch (Exception e) {
            log.error("Unexpected exception while produce notification topic. notificationId: {}, exception: {}",
                    notificationMessage.getId(), e.getMessage(), e);
            throw new ApplicationException(InternalServerExceptionCode.UNKNOWN_EXCEPTION);
        }
    }
}
