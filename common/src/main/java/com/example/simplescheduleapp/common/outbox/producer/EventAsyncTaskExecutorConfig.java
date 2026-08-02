package com.example.simplescheduleapp.common.outbox.producer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class EventAsyncTaskExecutorConfig {

    public static final String EVENT_ASYNC_TASK_EXECUTOR = "eventAsyncTaskExecutor";

    // 가상 쓰레드
    @Bean(name = EVENT_ASYNC_TASK_EXECUTOR)
    public Executor eventAsyncTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setThreadNamePrefix("VTP-");
        executor.setVirtualThreads(true);
        executor.setTaskTerminationTimeout(30000); // Graceful Shutdown 30초
        return executor;
    }
}
