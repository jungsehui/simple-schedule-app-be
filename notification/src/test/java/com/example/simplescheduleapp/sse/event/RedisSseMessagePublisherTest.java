package com.example.simplescheduleapp.sse.event;

import com.example.simplescheduleapp.common.messaging.MessagePublisher;
import com.example.simplescheduleapp.notification.application.event.NotificationRequest;
import com.example.simplescheduleapp.redis.publisher.RedisSseMessagePublisher;
import com.example.simplescheduleapp.redis.topic.RedisChannels;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisSseMessagePublisherTest {

    private MessagePublisher messagePublisher;
    private ObjectMapper objectMapper;
    private RedisSseMessagePublisher publisher;

    @BeforeEach
    void setUp() {
        messagePublisher = mock(MessagePublisher.class);
        objectMapper = new JsonMapper();
        publisher = new RedisSseMessagePublisher(messagePublisher, objectMapper);
    }

    @Test
    void publish_메서드가_올바른_JSON을_보내는지_확인() {
        // given
        Long senderMemberId = 1L;
        Long targetMemberId = 123L;
        String title = "테스트 제목입니다";
        String body = "테스트 메시지입니다";
        NotificationRequest event = new NotificationRequest(senderMemberId, targetMemberId, title, body);

        // when
        publisher.publish(event);

        // then
        verify(messagePublisher).publish(eq(RedisChannels.SSE_NOTIFICATION), argThat((String json) -> {
            try {
                NotificationRequest deserializedEvent = objectMapper.readValue(json, NotificationRequest.class);

                assertEquals(senderMemberId, deserializedEvent.senderId());
                assertEquals(targetMemberId, deserializedEvent.targetId());
                assertEquals(title, deserializedEvent.title());
                assertEquals(body, deserializedEvent.body());
                return true;
            } catch (JacksonException e) {
                return false;
            }
        }));
    }
}
