package com.db_migrator.etl_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration for Background Tasks
 * Provides thread pools for:
 * - Metadata extraction (system scans)
 * - Cycle execution (data migration)
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Task executor for metadata extraction (system scans)
     */
    @Bean(name = "metadataExtractionTaskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("metadata-extraction-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Task executor for cycle execution (data migration)
     *
     * - Core pool: 2 threads (minimum always available)
     * - Max pool: 5 threads (maximum concurrent cycle executions)
     * - Queue: 100 (pending cycle executions)
     *
     * Allows up to 5 cycles to execute concurrently
     */
    @Bean(name = "cycleExecutionTaskExecutor")
    public Executor cycleExecutionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cycle-exec-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
