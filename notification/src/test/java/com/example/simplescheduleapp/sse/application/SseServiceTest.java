package com.example.simplescheduleapp.sse.application;

import com.example.simplescheduleapp.NotificationApplication;
import com.example.simplescheduleapp.notification.application.event.NotificationRequest;
import com.example.simplescheduleapp.redis.publisher.RedisSseMessagePublisher;
import com.example.simplescheduleapp.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.verify;

@SpringBootTest(classes = NotificationApplication.class)
class SseServiceTest extends ApplicationTest {

    @Autowired
    private SseService sseService;

    @MockitoBean
    private RedisSseMessagePublisher redisSseMessagePublisher;

    @Test
    void SSE_Service_의_sendSseNotification_호출_시_발행되는지_테스트() {
        // given
        NotificationRequest message = new NotificationRequest(
                123L, 1234L, "test-topic", "test-body"
        );

        // when
        sseService.sendSseNotification(message);

        // then
        verify(redisSseMessagePublisher).publish(
                new NotificationRequest(
                        message.senderId(),
                        message.targetId(),
                        message.title(),
                        message.body()
                )
        );
    }
}
