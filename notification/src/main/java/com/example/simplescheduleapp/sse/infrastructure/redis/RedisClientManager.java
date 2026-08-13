package com.example.simplescheduleapp.sse.infrastructure.redis;

import com.example.simplescheduleapp.common.redis.presence.PresenceManager;
import com.example.simplescheduleapp.sse.application.port.out.SseClientPresence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisClientManager implements SseClientPresence {

    private static final String ONLINE_KEY_PREFIX = "online:";
    private static final Duration CONNECTION_TTL = Duration.ofSeconds(30);

    private final PresenceManager presenceManager;

    @Override
    public void subscribeClient(Long memberId) {
        String key = getUserKey(memberId);
        presenceManager.markOnline(key, CONNECTION_TTL);
        log.info("사용자 연결 등록 - memberId: {}, key: {}", memberId, key);
    }

    @Override
    public void unsubscribeClient(Long memberId) {
        String key = getUserKey(memberId);
        presenceManager.markOffline(key);
        log.info("사용자 연결 해제 - memberId: {}, key: {}", memberId, key);
    }

    @Override
    public boolean isClientConnected(Long memberId) {
        String key = getUserKey(memberId);
        boolean isConnected = presenceManager.isOnline(key);
        log.debug("사용자 연결 상태 확인 - memberId: {}, isConnected: {}", memberId, isConnected);
        return isConnected;
    }

    @Override
    public void refreshConnection(Long memberId) {
        String key = getUserKey(memberId);
        presenceManager.refreshTtl(key, CONNECTION_TTL);
        log.debug("사용자 heartbeat TTL 갱신 - memberId: {}, TTL: {}초", memberId, CONNECTION_TTL.getSeconds());
    }

    private String getUserKey(Long memberId) {
        return ONLINE_KEY_PREFIX + memberId;
    }
}
