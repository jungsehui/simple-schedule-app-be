package com.example.simplescheduleapp.sse.config;

import com.example.simplescheduleapp.sse.event.RedisSseMessageSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class RedisListenerConfig {

    private final RedisSseMessageSubscriber redisSseMessageSubscriber;

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(redisSseMessageSubscriber, new PatternTopic("sse-notification"));
        return container;
    }
}
