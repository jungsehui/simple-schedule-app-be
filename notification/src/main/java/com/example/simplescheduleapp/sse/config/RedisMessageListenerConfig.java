package com.example.simplescheduleapp.sse.config;

import com.example.simplescheduleapp.sse.infrastructure.redis.RedisSseMessageSubscriber;
import com.example.simplescheduleapp.sse.infrastructure.redis.RedisChannels;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@RequiredArgsConstructor
@Configuration
public class RedisMessageListenerConfig {

    private final RedisSseMessageSubscriber redisSseMessageSubscriber;

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(redisSseMessageSubscriber, PatternTopic.of(RedisChannels.SSE_NOTIFICATION));
        return container;
    }
}
