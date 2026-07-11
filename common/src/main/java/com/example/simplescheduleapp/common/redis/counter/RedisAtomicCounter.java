package com.example.simplescheduleapp.common.redis.counter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisAtomicCounter implements AtomicCounter {

    /**
     * 원자적 check-and-decrement — 키 없음(-2)과 소진(-1)을 구분해 반환.
     */
    private static final DefaultRedisScript<Long> DECREMENT_IF_POSITIVE_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('get', KEYS[1])
            if current == false then return -2 end
            if tonumber(current) <= 0 then return -1 end
            return redis.call('decr', KEYS[1])
            """,
            Long.class
    );

    /**
     * 키가 존재할 때만 증가 — 만료 키를 TTL 없이 부활시키지 않는다.
     */
    private static final DefaultRedisScript<Long> INCREMENT_IF_EXISTS_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('get', KEYS[1])
            if current == false then return -2 end
            return redis.call('incr', KEYS[1])
            """,
            Long.class
    );

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

    @Override
    public Long decrementIfPositive(String key) {
        return stringRedisTemplate.execute(DECREMENT_IF_POSITIVE_SCRIPT, List.of(key));
    }

    @Override
    public Long incrementIfExists(String key) {
        return stringRedisTemplate.execute(INCREMENT_IF_EXISTS_SCRIPT, List.of(key));
    }
}
