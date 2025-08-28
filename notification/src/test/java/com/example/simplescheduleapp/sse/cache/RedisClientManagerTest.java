package com.example.simplescheduleapp.sse.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisClientManagerTest {

    private RedisClientManager redisClientManager;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        redisClientManager = new RedisClientManager(redisTemplate);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void subscribeClient_호출_시_redis에_set_호출된다() {
        Long memberId = 1L;

        redisClientManager.subscribeClient(memberId);

        verify(valueOperations).set(eq("online:1"), eq("true"), eq(Duration.ofSeconds(30)));
    }

    @Test
    void unsubscribeClient_호출_시_redis에_delete_호출된다() {
        Long memberId = 2L;

        redisClientManager.unsubscribeClient(memberId);

        verify(redisTemplate).delete("online:2");
    }

    @Test
    void isClientConnected_호출_시_redis에_key_존재하면_true_반환() {
        Long memberId = 3L;
        when(redisTemplate.hasKey("online:3")).thenReturn(true);

        boolean result = redisClientManager.isClientConnected(memberId);

        assertThat(result).isTrue();
    }

    @Test
    void isClientConnected_호출_시_key_없으면_false_반환() {
        Long memberId = 4L;
        when(redisTemplate.hasKey("online:4")).thenReturn(false);

        boolean result = redisClientManager.isClientConnected(memberId);

        assertThat(result).isFalse();
    }

    @Test
    void refreshConnection_호출_시_TTL_갱신을_위해_set_호출된다() {
        Long memberId = 5L;

        redisClientManager.refreshConnection(memberId);

        verify(valueOperations).set(eq("online:5"), eq("true"), eq(Duration.ofSeconds(30)));
    }
}
