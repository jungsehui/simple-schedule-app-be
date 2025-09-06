package com.example.simplescheduleapp.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ThreadPoolConfig {

    public static final String NOTIFICATION_TASK_EXECUTOR = "notificationExecutor";

    @Bean(name = NOTIFICATION_TASK_EXECUTOR)
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(200);      // 기본 스레드 수
        executor.setMaxPoolSize(200);       // 최대 스레드 수
        executor.setQueueCapacity(100_000); // 대기 큐 크기
        executor.setThreadNamePrefix("Notification-");
        executor.initialize();
        return executor;
    }
}
