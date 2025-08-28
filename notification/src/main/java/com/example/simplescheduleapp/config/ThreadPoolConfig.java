package com.example.simplescheduleapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadPoolConfig {

    @Bean("notificationTaskExecutor")
    public TaskExecutor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);   // 기본 스레드 수
        executor.setMaxPoolSize(20);    // 최대 스레드 수
        executor.setQueueCapacity(100); // 대기 큐 크기
        executor.setThreadNamePrefix("Notification-");
        executor.initialize();
        return executor;
    }
}
