package com.database_migrator.domain.execution.service;

import com.database_migrator.domain.execution.model.Cycle;
import com.database_migrator.domain.execution.model.Task;
import com.database_migrator.domain.execution.model.CycleStatusEnum;
import com.database_migrator.domain.execution.model.TaskStatusEnum;
import com.database_migrator.domain.execution.repository.TaskRepository;
import com.database_migrator.domain.common.util.TransformationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CycleExecutionService {

    private static final int THREAD_POOL_SIZE = 10;

    private final TaskRepository taskRepository;

    private enum ExecutionMode {
        SEQUENTIAL,
        PARALLEL
    }

    public void executeBatches(Deque<List<Task>> taskBatches, Cycle cycle, TaskExecutor taskExecutor, boolean forceSequential) {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try {
            int batchNumber = 0;

            while (!taskBatches.isEmpty()) {
                List<Task> batch = taskBatches.pollFirst();
                batchNumber++;

                ExecutionMode mode = forceSequential ? ExecutionMode.SEQUENTIAL : ExecutionMode.PARALLEL;
                log.info("Cycle {}: Executing batch {} with {} tasks (mode: {})",
                        cycle.getId(), batchNumber, batch.size(), mode);

                if (forceSequential) {
                    executeSequentially(batch, cycle, taskExecutor, taskBatches);
                } else {
                    boolean batchFailed = executeInParallel(batch, cycle, taskExecutor, executorService, batchNumber);
                    if (batchFailed) {
                        markCycleAsFailed(cycle, batchNumber);
                        cancelRemainingTasks(taskBatches);
                        break;
                    }
                }

                log.info("Cycle {}: Batch {} completed successfully", cycle.getId(), batchNumber);
            }

        } finally {
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
     * Executes tasks one by one in the current thread.
     * Used for MSSQL targets due to IDENTITY_INSERT limitation.
     */
    private void executeSequentially(List<Task> batch, Cycle cycle, TaskExecutor taskExecutor, Deque<List<Task>> remainingBatches) {
        for (Task task : batch) {
            try {
                executeTask(task, cycle, taskExecutor);
            } catch (Exception e) {
                log.error("Cycle {}: Sequential task {} failed", cycle.getId(), task.getId());
                markCycleAsFailed(cycle, task.getId());
                cancelRemainingTasks(remainingBatches);
                return;
            }
        }
    }

    /**
     * Submits all tasks in batch to thread pool for concurrent execution.
     */
    private boolean executeInParallel(List<Task> batch, Cycle cycle, TaskExecutor taskExecutor,
                                      ExecutorService executorService, int batchNumber) {
        List<Future<?>> futures = new ArrayList<>();

        for (Task task : batch) {
            futures.add(executorService.submit(() -> executeTask(task, cycle, taskExecutor)));
        }

        return waitForBatchCompletion(futures, cycle, batchNumber);
    }

    /**
     * Executes a single table migration task (DDL + data migration).
     */
    private void executeTask(Task task, Cycle cycle, TaskExecutor taskExecutor) {
        Long taskId = task.getId();
        String tableName = TransformationUtils.getEffectiveTableName(task.getTransformationTable());

        try {
            log.info("Cycle {}: Starting task {} (table: {})", cycle.getId(), taskId, tableName);
            taskExecutor.executeTaskById(taskId);
            log.info("Cycle {}: Completed task {} (table: {})", cycle.getId(), taskId, tableName);
        } catch (Exception e) {
            log.error("Cycle {}: Task {} (table: {}) failed: {}", cycle.getId(), taskId, tableName, e.getMessage(), e);
            throw new com.database_migrator.domain.common.exception.ExecutionException("Task execution failed", e);
        }
    }

    private boolean waitForBatchCompletion(List<Future<?>> futures, Cycle cycle, int batchNumber) {
        boolean batchFailed = false;

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                log.error("Cycle {}: Task interrupted in batch {}", cycle.getId(), batchNumber);
                batchFailed = true;
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                log.error("Cycle {}: Task failed in batch {}: {}", cycle.getId(), batchNumber, e.getCause().getMessage());
                batchFailed = true;
            }
        }

        return batchFailed;
    }

    private void markCycleAsFailed(Cycle cycle, Object context) {
        cycle.setStatus(CycleStatusEnum.FAILED);
        cycle.setErrorMessage(context + " failed. Check task logs for details.");
    }

    private void cancelRemainingTasks(Deque<List<Task>> remainingBatches) {
        List<Task> tasksToCancel = new ArrayList<>();

        while (!remainingBatches.isEmpty()) {
            List<Task> batch = remainingBatches.pollFirst();
            for (Task task : batch) {
                task.setStatus(TaskStatusEnum.FAILED);
                task.setErrorMessage("Cancelled due to previous batch failure");
                task.setCompletedAt(new Date());
                tasksToCancel.add(task);
            }
        }

        if (!tasksToCancel.isEmpty()) {
            taskRepository.saveAll(tasksToCancel);
            log.warn("Cancelled {} remaining tasks due to batch failure", tasksToCancel.size());
        }
    }
}
