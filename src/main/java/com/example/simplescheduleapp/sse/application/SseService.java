package com.example.simplescheduleapp.sse.application;

import com.example.simplescheduleapp.sse.event.RedisSseMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class SseService {

    private final RedisSseMessagePublisher redisSseMessagePublisher;

    public void sendNotification(Long memberId, String eventName, String message) {
        log.info("Redis 를 통해 memberId: {} 에게 이벤트 발행 - event: {}, message: {}", memberId, eventName, message);
        redisSseMessagePublisher.publish(memberId, eventName, message);
    }
}
