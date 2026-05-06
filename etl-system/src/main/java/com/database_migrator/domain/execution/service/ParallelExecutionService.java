package com.database_migrator.domain.execution.service;

import com.database_migrator.domain.execution.model.Cycle;
import com.database_migrator.domain.execution.model.Task;
import com.database_migrator.domain.execution.model.CycleStatusEnum;
import com.database_migrator.domain.execution.model.TaskStatusEnum;
import com.database_migrator.domain.common.util.TransformationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Parallel Execution Service using BTC Pattern
 * Executes task batches in parallel using ExecutorService:
 * 1. Fixed thread pool (10 threads)
 * 2. Tasks in same batch run in PARALLEL
 * 3. Wait for entire batch to complete before next batch (Future.get() synchronization)
 * 4. Stop execution on batch failure (fail fast)
 * Benefits:
 * - Automatic thread management
 * - Parallel execution within batch
 * - Sequential batch execution
 * - Exception handling with Future
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParallelExecutionService {

    private static final int THREAD_POOL_SIZE = 10;

    /**
     * Execute task batches in parallel
     *
     * @param taskBatches  Ordered batches (FIFO)
     * @param cycle        Execution cycle
     * @param taskExecutor Task executor service
     */
    public void executeBatches(Deque<List<Task>> taskBatches, Cycle cycle, TaskExecutor taskExecutor) {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try {
            int batchNumber = 0;

            while (!taskBatches.isEmpty()) {
                List<Task> batch = taskBatches.pollFirst(); // FIFO order
                batchNumber++;

                log.info("Cycle {}: Executing batch {} with {} tasks",
                        cycle.getId(), batchNumber, batch.size());

                // Submit all tasks in batch to thread pool
                List<Future<?>> futures = new ArrayList<>();

                for (Task task : batch) {
                    Long taskId = task.getId();  // Extract ID before passing to background thread

                    futures.add(executorService.submit(() -> {
                        try {
                            String tableName = TransformationUtils
                                    .getEffectiveTableName(task.getTransformationTable());

                            log.info("Cycle {}: Starting task {} (table: {})",
                                    cycle.getId(), taskId, tableName);

                            // Execute task by ID (fetches fresh in its own transaction)
                            taskExecutor.executeTaskById(taskId);

                            log.info("Cycle {}: Completed task {} (table: {})",
                                    cycle.getId(), taskId, tableName);

                        } catch (Exception e) {
                            String tableName = TransformationUtils
                                    .getEffectiveTableName(task.getTransformationTable());

                            log.error("Cycle {}: Task {} (table: {}) failed: {}",
                                    cycle.getId(), taskId, tableName, e.getMessage(), e);

                            // Re-throw to fail the Future
                            throw new RuntimeException("Task execution failed", e);
                        }
                    }));
                }

                // Wait for all tasks in batch to complete (synchronization point)
                boolean batchFailed = false;
                for (Future<?> future : futures) {
                    try {
                        future.get(); // Blocks until task completes

                    } catch (InterruptedException e) {
                        log.error("Cycle {}: Task interrupted in batch {}", cycle.getId(), batchNumber);
                        batchFailed = true;
                        Thread.currentThread().interrupt();

                    } catch (ExecutionException e) {
                        log.error("Cycle {}: Task failed in batch {}: {}",
                                cycle.getId(), batchNumber, e.getCause().getMessage());
                        batchFailed = true;
                    }
                }

                // Stop execution if batch failed (fail fast)
                if (batchFailed) {
                    log.error("Cycle {}: Batch {} failed, stopping execution", cycle.getId(), batchNumber);

                    // Mark cycle as FAILED
                    cycle.setStatus(CycleStatusEnum.FAILED);
                    cycle.setErrorMessage("Batch " + batchNumber + " failed. Check task logs for details.");

                    // Cancel remaining tasks
                    cancelRemainingTasks(taskBatches);

                    break;
                }

                log.info("Cycle {}: Batch {} completed successfully", cycle.getId(), batchNumber);
            }

        } finally {
            // Shutdown executor service
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Cancel remaining tasks in queue
     */
    private void cancelRemainingTasks(Deque<List<Task>> remainingBatches) {
        int cancelledCount = 0;

        while (!remainingBatches.isEmpty()) {
            List<Task> batch = remainingBatches.pollFirst();
            for (Task task : batch) {
                task.setStatus(TaskStatusEnum.FAILED);
                task.setErrorMessage("Cancelled due to previous batch failure");
                cancelledCount++;
            }
        }

        if (cancelledCount > 0) {
            log.warn("Cancelled {} remaining tasks", cancelledCount);
        }
    }
}
