package com.example.playground.async.threadpool;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ThreadPoolAsyncConfig {

    public static final String THREAD_POOL_ASYNC_TASK_EXECUTOR = "threadPoolAsyncTaskExecutor";

    @Bean(name = THREAD_POOL_ASYNC_TASK_EXECUTOR)
    public Executor singleThreadAsyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(200);
        executor.setMaxPoolSize(200);
        executor.setQueueCapacity(10_000_000);
        executor.setThreadNamePrefix("TP-");
        executor.initialize();
        return executor;
    }
}
