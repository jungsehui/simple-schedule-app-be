package com.example.simplescheduleapp.common.redis.presence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisPresenceManager implements PresenceManager {

    private static final String ONLINE_VALUE = "1";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void markOnline(String key, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key, ONLINE_VALUE, ttl);
    }

    @Override
    public void markOffline(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    public boolean isOnline(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    @Override
    public void refreshTtl(String key, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key, ONLINE_VALUE, ttl);
    }
}
