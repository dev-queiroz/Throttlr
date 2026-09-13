package com.example.ratelimiter.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class VirtualThreadConfig {

    @Bean(destroyMethod = "close")
    Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    AsyncTaskExecutor applicationTaskExecutor(Executor virtualThreadExecutor) {
        return new TaskExecutorAdapter(virtualThreadExecutor);
    }
}
