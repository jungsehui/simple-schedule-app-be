package com.example.simplescheduleapp.common.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    private static final String REDISSON_HOST_PREFIX = "redis://";

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    // requirepass가 걸린 운영 Redis 대응 — 미설정(빈 값)이면 무인증 로컬/CI와 동일 동작
    @Value("${spring.data.redis.password:}")
    private String password;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        SingleServerConfig singleServer = config.useSingleServer()
                .setAddress(REDISSON_HOST_PREFIX + host + ":" + port);
        if (!password.isBlank()) {
            singleServer.setPassword(password);
        }
        return Redisson.create(config);
    }
}
