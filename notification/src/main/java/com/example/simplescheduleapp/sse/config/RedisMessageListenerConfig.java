package com.example.simplescheduleapp.sse.config;

import com.example.simplescheduleapp.redis.subscriber.RedisSseMessageSubscriber;
import com.example.simplescheduleapp.redis.topic.RedisChannels;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@RequiredArgsConstructor
@Configuration
public class RedisListenerConfig {

    private final RedisSseMessageSubscriber redisSseMessageSubscriber;

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {
        PatternTopic patternTopic = new PatternTopic(RedisChannels.SSE_NOTIFICATION);
        ChannelTopic channelTopic = new ChannelTopic(RedisChannels.SSE_NOTIFICATION);
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(redisSseMessageSubscriber, new ChannelTopic(RedisChannels.SSE_NOTIFICATION));
        return container;
    }
}
