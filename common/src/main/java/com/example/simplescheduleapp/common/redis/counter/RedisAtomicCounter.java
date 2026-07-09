package com.example.simplescheduleapp.common.redis.counter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisAtomicCounter implements AtomicCounter {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void set(String key, long value) {
        stringRedisTemplate.opsForValue().set(key, String.valueOf(value));
    }

    @Override
    public void set(String key, long value, Duration ttl) {
        Objects.requireNonNull(ttl, "ttl must not be null");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive: " + ttl);
        }
        // SET key value EX seconds — 원자적 SET + EXPIRE
        stringRedisTemplate.opsForValue().set(key, String.valueOf(value), ttl);
    }

    @Override
    public Long decrement(String key) {
        return stringRedisTemplate.opsForValue().decrement(key);
    }

    @Override
    public Long increment(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }
}
