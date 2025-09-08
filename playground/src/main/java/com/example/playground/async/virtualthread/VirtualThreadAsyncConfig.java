package com.example.playground.async.virtualthread;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class VirtualThreadAsyncConfig {

    public static final String VIRTUAL_THREAD_ASYNC_TASK_EXECUTOR = "virtualThreadAsyncTaskExecutor";

    @Bean(name = VIRTUAL_THREAD_ASYNC_TASK_EXECUTOR)
    public Executor virtualThreadAsyncTaskExecutor() {
        ExecutorService concurrentExecutor = Executors.newVirtualThreadPerTaskExecutor();
        return new TaskExecutorAdapter(concurrentExecutor);
    }
}
