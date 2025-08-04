package com.example.simplescheduleapp.sse.application;

import com.example.simplescheduleapp.kafka.event.NotificationMessageEvent;
import com.example.simplescheduleapp.sse.event.RedisSseMessagePublisher;
import com.example.simplescheduleapp.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.verify;

class SseServiceTest extends ApplicationTest {

    @Autowired
    private SseService sseService;

    @MockitoBean
    private RedisSseMessagePublisher redisSseMessagePublisher;

    @Test
    void SSE_Service_의_sendSseNotification_호출_시_퍼블리시_되는지_테스트() {
        // given
        NotificationMessageEvent message = new NotificationMessageEvent(
                123L, 1234L, "test-topic", "test-body"
        );

        // when
        sseService.sendSseNotification(message);

        // then
        verify(redisSseMessagePublisher).publish(
                new NotificationMessageEvent(
                        message.senderMemberId(),
                        message.targetMemberId(),
                        message.title(),
                        message.body()
                )
        );
    }
}
