package com.example.simplescheduleapp.kafka.producer;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.InternalServerExceptionCode;
import com.example.simplescheduleapp.common.kafka.producer.KafkaProducer;
import com.example.simplescheduleapp.kafka.topic.NotificationDispatchTopicMessage;
import com.example.simplescheduleapp.notification.domain.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class SendNotificationProducer {

    public static final String SEND_NOTIFICATION_TOPIC = "READ_CHAT";

    private final KafkaProducer<NotificationDispatchTopicMessage> kafkaProducer;

    public void produce(NotificationMessage notificationMessage) {
        try {
            NotificationDispatchTopicMessage message = NotificationDispatchTopicMessage.from(notificationMessage);
            log.info("Try to produce read domain topic. messageId: {}", notificationMessage.getId());
            kafkaProducer.produce(SEND_NOTIFICATION_TOPIC, message);
            log.info("Successfully produce read domain info topic. messageId: {}", notificationMessage.getId());
        } catch (Exception e) {
            log.error("Unexpected exception while produce chat domain messageId: {}, exception domain: {}",
                    notificationMessage.getId(), e.getMessage(), e);
            throw new ApplicationException(InternalServerExceptionCode.UNKNOWN_EXCEPTION);
        }
    }
}
