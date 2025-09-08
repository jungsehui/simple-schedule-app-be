package com.example.playground.async.singlethread;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class SingleThreadAsyncConfig {

    public static final String SINGLE_THREAD_ASYNC_TASK_EXECUTOR = "singleThreadAsyncTaskExecutor";

    @Bean
    public Executor singleThreadAsyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10_000_000);
        executor.setThreadNamePrefix("ST-");
        executor.initialize();
        return executor;
    }
}
