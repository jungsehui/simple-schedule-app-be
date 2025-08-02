package com.example.simplescheduleapp.sse.event;

import com.example.simplescheduleapp.fcm.application.FcmService;
import com.example.simplescheduleapp.notification.message.NotificationMessage;
import com.example.simplescheduleapp.sse.domain.RedisClientManager;
import com.example.simplescheduleapp.sse.domain.SseEmitterRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisSseMessageSubscriber implements MessageListener {

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;
    private static final long INITIAL_DELAY = 10L;
    private static final long PERIOD = 10L;

    private final FcmService fcmService;
    private final SseEmitterRepository sseEmitterRepository;
    private final RedisClientManager redisClientManager;

    public SseEmitter connect(Long memberId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        sseEmitterRepository.save(memberId, emitter);
        redisClientManager.subscribeClient(memberId);
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE connected for memberId: " + memberId));
            sendHeartbeat(memberId, emitter);
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        onCompletion(memberId, emitter);
        onTimeout(memberId, emitter);
        onError(memberId, emitter);
        return emitter;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> map = mapper.readValue(body, new TypeReference<>() {});
            Long memberId = Long.valueOf(map.get("memberId"));
            String eventName = map.get("eventName");
            String messageBody = map.get("messageBody");
            sendSseMessage(new NotificationMessage(memberId, eventName, messageBody));
        } catch (JsonProcessingException e) {
            log.error("Redis 메시지 파싱 오류: {}", e.getMessage());
        }
    }

    private void onError(Long memberId, SseEmitter emitter) {
        emitter.onError((ex) -> {
            sseEmitterRepository.delete(memberId);
            emitter.completeWithError(ex);
            redisClientManager.unsubscribeClient(memberId); // Redis Pub/Sub 구독 해지
        });
    }

    private void onTimeout(Long memberId, SseEmitter emitter) {
        emitter.onTimeout(() -> {
            sseEmitterRepository.delete(memberId);
            emitter.complete();
            redisClientManager.unsubscribeClient(memberId); // Redis Pub/Sub 구독 해지
        });
    }

    private void onCompletion(Long memberId, SseEmitter emitter) {
        emitter.onCompletion(() -> {
            sseEmitterRepository.delete(memberId);
            log.debug("SSE Emitter 알림 성공 - memberId: {}", memberId);
        });
    }

    private void sendHeartbeat(Long memberId, SseEmitter emitter) {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("연결 끊김 방지"));
                redisClientManager.refreshConnection(memberId);
            } catch (IOException e) {
                emitter.complete();
                sseEmitterRepository.delete(memberId);
                redisClientManager.unsubscribeClient(memberId);
            }
        }, INITIAL_DELAY, PERIOD, TimeUnit.SECONDS);
    }

    private void sendSseMessage(NotificationMessage message) {
        SseEmitter emitter = sseEmitterRepository.get(message.memberId());
        try {
            emitter.send(SseEmitter.event()
                    .name(message.eventName())
                    .data(message.messageBody()));
            log.info("SSE 이벤트 전송 성공 - memberId: {}, event: {}, message: {}",
                    message.memberId(), message.eventName(), message.messageBody());
        } catch (IOException e) {
            log.error("memberId: {} 에게 SSE 이벤트 전송 실패: {}", message.memberId(), e.getMessage());
            sseEmitterRepository.delete(message.memberId());
            emitter.completeWithError(e);
            fcmFallback(message);
        }
    }

    // SSE 기능 동작 실패 시 FCM 으로 메시지 발행
    private void fcmFallback(NotificationMessage message) {
        log.info("FCM Fallback memberId: {}", message.memberId());
        fcmService.sendPushNotification(message);
    }
}
