package com.example.simplescheduleapp.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AsyncConfig {

    public static final String SPECIAL_LECTURE_ENROLLMENT_ASYNC_TASK_EXECUTOR = "specialLectureEnrollmentAsyncTaskExecutor";

    // 가상 쓰레드
    @Bean(name = SPECIAL_LECTURE_ENROLLMENT_ASYNC_TASK_EXECUTOR)
    public Executor eventAsyncTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setThreadNamePrefix("VTP-");
        executor.setVirtualThreads(true);
        executor.setTaskTerminationTimeout(30000); // Graceful Shutdown 30초
        return executor;
    }
}
