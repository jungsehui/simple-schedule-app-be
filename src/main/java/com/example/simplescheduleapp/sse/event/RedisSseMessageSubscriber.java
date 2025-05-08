package com.example.simplescheduleapp.sse.event;

import com.example.simplescheduleapp.fcm.application.FcmService;
import com.example.simplescheduleapp.fcm.domain.FcmToken;
import com.example.simplescheduleapp.fcm.domain.FcmTokenRepository;
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

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisSseMessageSubscriber implements MessageListener {

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    private final FcmService fcmService;
    private final SseEmitterRepository sseEmitterRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final RedisClientManager redisClientManager;

    public SseEmitter connect(Long memberId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        sseEmitterRepository.save(memberId, emitter);
        redisClientManager.subscribeClient(memberId);

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE connected for memberId: " + memberId));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        emitter.onCompletion(() -> {
            sseEmitterRepository.delete(memberId);
            redisClientManager.unsubscribeClient(memberId); // Redis Pub/Sub 구독 해지
        });
        emitter.onTimeout(() -> {
            sseEmitterRepository.delete(memberId);
            emitter.complete();
            redisClientManager.unsubscribeClient(memberId); // Redis Pub/Sub 구독 해지
        });
        emitter.onError((ex) -> {
            sseEmitterRepository.delete(memberId);
            emitter.completeWithError(ex);
            redisClientManager.unsubscribeClient(memberId); // Redis Pub/Sub 구독 해지
        });

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
            String content = map.get("message");
            sendSseMessage(memberId, eventName, content);
        } catch (JsonProcessingException e) {
            log.error("Redis 메시지 파싱 오류: {}", e.getMessage());
        }
    }

    private void sendSseMessage(Long memberId, String eventName, String message) {
        SseEmitter emitter = sseEmitterRepository.get(memberId);
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(message));
            log.info("SSE 이벤트 전송 성공 - memberId: {}, event: {}, message: {}", memberId, eventName, message);
        } catch (IOException e) {
            log.error("memberId: {} 에게 SSE 이벤트 전송 실패: {}", memberId, e.getMessage());
            sseEmitterRepository.delete(memberId);
            emitter.completeWithError(e);
            fcmFallback(memberId, eventName, message);
        }
    }

    // 혹시 모를 SSE 기능 동작 실패 시 FCM 으로 메시지 발행
    private void fcmFallback(Long memberId, String eventName, String message) {
        FcmToken fcmToken = fcmTokenRepository.getByMemberId(memberId);
        if (fcmToken != null) {
            fcmService.sendPushNotification(String.valueOf(fcmToken), eventName, message);
        } else {
            log.error("memberId: {} 에 대한 FCM 토큰이 없습니다.", memberId);
        }
    }
}
