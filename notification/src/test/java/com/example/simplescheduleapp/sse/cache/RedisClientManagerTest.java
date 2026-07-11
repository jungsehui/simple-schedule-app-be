package com.example.simplescheduleapp.sse.cache;

import com.example.simplescheduleapp.common.redis.presence.PresenceManager;
import com.example.simplescheduleapp.redis.cache.RedisClientManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisClientManagerTest {

    private RedisClientManager redisClientManager;

    @Mock
    private PresenceManager presenceManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        redisClientManager = new RedisClientManager(presenceManager);
    }

    @Test
    void subscribeClient_호출_시_redis에_set_호출된다() {
        Long memberId = 1L;

        redisClientManager.subscribeClient(memberId);

        verify(presenceManager).markOnline(eq("online:1"), eq(Duration.ofSeconds(30)));
    }

    @Test
    void unsubscribeClient_호출_시_redis에_delete_호출된다() {
        Long memberId = 2L;

        redisClientManager.unsubscribeClient(memberId);

        verify(presenceManager).markOffline("online:2");
    }

    @Test
    void isClientConnected_호출_시_redis에_key_존재하면_true_반환() {
        Long memberId = 3L;
        when(presenceManager.isOnline("online:3")).thenReturn(true);

        boolean result = redisClientManager.isClientConnected(memberId);

        assertThat(result).isTrue();
    }

    @Test
    void isClientConnected_호출_시_key_없으면_false_반환() {
        Long memberId = 4L;
        when(presenceManager.isOnline("online:4")).thenReturn(false);

        boolean result = redisClientManager.isClientConnected(memberId);

        assertThat(result).isFalse();
    }

    @Test
    void refreshConnection_호출_시_TTL_갱신을_위해_set_호출된다() {
        Long memberId = 5L;

        redisClientManager.refreshConnection(memberId);

        verify(presenceManager).refreshTtl(eq("online:5"), eq(Duration.ofSeconds(30)));
    }
}
