package com.example.simplescheduleapp.sse.application;

import com.example.simplescheduleapp.notification.message.NotificationMessage;
import com.example.simplescheduleapp.sse.event.RedisSseMessagePublisher;
import com.example.simplescheduleapp.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.*;

class SseServiceTest extends ApplicationTest {

    @Autowired
    private SseService sseService;

    @MockitoBean
    private RedisSseMessagePublisher redisSseMessagePublisher;

    @Test
    void SSE_Service_의_sendNotification_호출_시_퍼블리시_되는지_테스트() {
        // given
        NotificationMessage message = new NotificationMessage(
                123L, "test-event", "Test, SSE 메시지입니당 !!"
        );

        // when
        sseService.sendNotification(message);

        // then
        verify(redisSseMessagePublisher).publish(123L, "test-event", "Test, SSE 메시지입니당 !!");
    }
}
