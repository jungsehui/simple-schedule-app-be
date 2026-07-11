package com.example.simplescheduleapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

@Configuration
public class ThreadPoolConfig {

    public static final String NOTIFICATION_TASK_EXECUTOR = "notificationExecutor";
    public static final String FAILED_NOTIFICATION_TASK_EXECUTOR = "failedNotificationExecutor";
    public static final String SSE_HEARTBEAT_SCHEDULER = "sseHeartbeatScheduler";

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

    @Bean(name = FAILED_NOTIFICATION_TASK_EXECUTOR)
    public Executor failedNotificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);       // 기본 스레드 수
        executor.setMaxPoolSize(5);        // 최대 스레드 수
        executor.setThreadNamePrefix("Failed-Notification-");
        executor.initialize();
        return executor;
    }

    @Bean(name = SSE_HEARTBEAT_SCHEDULER)
    public TaskScheduler sseHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);          // 적절한 사이즈 설정
        scheduler.setThreadNamePrefix("SSE-Heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}
