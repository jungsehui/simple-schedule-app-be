package com.example.simplescheduleapp.sse.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisClientManager {

    private static final String SSE_CHANNEL_PREFIX = "sse:user:";

    private final RedisTemplate<String, String> redisTemplate;

    public void subscribeClient(Long memberId) {
        String sseChannel = SSE_CHANNEL_PREFIX + memberId;
        redisTemplate.opsForSet().add(sseChannel, "subscriber:" + memberId);
        log.info("Redis Pub/Sub 채널 구독 - memberId: {}, channel: {}", memberId, sseChannel);
    }

    public void unsubscribeClient(Long memberId) {
        String sseChannel = SSE_CHANNEL_PREFIX + memberId;
        redisTemplate.opsForSet().remove(sseChannel, "subscriber:" + memberId);
        log.info("Redis Pub/Sub 채널 구독 해지 - memberId: {}, channel: {}", memberId, sseChannel);
    }

    public boolean isClientConnected(Long memberId) {
        String sseChannel = SSE_CHANNEL_PREFIX + memberId;
        boolean isConnected = Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(sseChannel, "subscriber:" + memberId)
        );
        log.debug("사용자 연결 상태 확인 - memberId: {}, isConnected: {}", memberId, isConnected);
        return isConnected;
    }
}
