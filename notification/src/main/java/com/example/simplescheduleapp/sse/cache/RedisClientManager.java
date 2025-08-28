package com.example.simplescheduleapp.sse.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisClientManager {

    private static final String ONLINE_KEY_PREFIX = "online:";
    private static final Duration CONNECTION_TTL = Duration.ofSeconds(30);

    private final RedisTemplate<String, String> redisTemplate;

    public void subscribeClient(Long memberId) {
        String key = getUserKey(memberId);
        redisTemplate.opsForValue().set(key, "true", CONNECTION_TTL);
        log.info("사용자 연결 등록 - memberId: {}, key: {}", memberId, key);
    }

    public void unsubscribeClient(Long memberId) {
        String key = getUserKey(memberId);
        redisTemplate.delete(key);
        log.info("사용자 연결 해제 - memberId: {}, key: {}", memberId, key);
    }

    public boolean isClientConnected(Long memberId) {
        String key = getUserKey(memberId);
        Boolean exists = redisTemplate.hasKey(key);
        boolean isConnected = Boolean.TRUE.equals(exists);
        log.debug("사용자 연결 상태 확인 - memberId: {}, isConnected: {}", memberId, isConnected);
        return isConnected;
    }

    public void refreshConnection(Long memberId) {
        String key = getUserKey(memberId);
        redisTemplate.opsForValue().set(key, "true", CONNECTION_TTL);
        log.debug("사용자 heartbeat TTL 갱신 - memberId: {}, TTL: {}초", memberId, CONNECTION_TTL.getSeconds());
    }

    private String getUserKey(Long memberId) {
        return ONLINE_KEY_PREFIX + memberId;
    }
}
