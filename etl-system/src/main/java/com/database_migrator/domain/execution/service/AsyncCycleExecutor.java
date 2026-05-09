package com.database_migrator.domain.execution.service;

import com.database_migrator.domain.common.exception.ResourceNotFoundException;
import com.database_migrator.domain.execution.model.Cycle;
import com.database_migrator.domain.execution.model.Task;
import com.database_migrator.domain.execution.model.CycleStatusEnum;
import com.database_migrator.domain.execution.model.TaskStatusEnum;
import com.database_migrator.domain.execution.repository.CycleRepository;
import com.database_migrator.domain.execution.repository.TaskRepository;
import com.database_migrator.domain.common.exception.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncCycleExecutor {

    private final CycleRepository cycleRepository;
    private final TaskRepository taskRepository;
    private final TaskScheduler taskScheduler;
    private final CycleExecutionService cycleExecutionService;
    private final TaskExecutor taskExecutor;

    @Async("cycleExecutionTaskExecutor")
    @Transactional
    public void executeAsync(Long cycleId) {
        log.info("Cycle {}: Starting async execution in background thread", cycleId);

        // Fetch cycle with tasks eagerly loaded
        Cycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle", cycleId));

        try {
            // Build task graph
            Map<Task, Set<Task>> taskGraph = taskScheduler.buildTaskGraph(cycle);

            // Compute execution batches based on dependencies
            Deque<List<Task>> taskBatches = taskScheduler.computeExecutionBatches(taskGraph);

            log.info("Cycle {}: Executing {} batches", cycle.getId(), taskBatches.size());

            // Check if target is MSSQL - force sequential execution due to IDENTITY_INSERT limitation
            // MSSQL only allows ONE table to have IDENTITY_INSERT ON at a time (database-wide lock)
            boolean forceSequential = isMssqlTarget(cycle);
            if (forceSequential) {
                log.warn("Cycle {}: Target is MSSQL - forcing SEQUENTIAL execution due to IDENTITY_INSERT limitation",
                        cycle.getId());
            }

            cycleExecutionService.executeBatches(taskBatches, cycle, taskExecutor, forceSequential);

            updateCycleStatus(cycle);

            log.info("Cycle {}: Execution completed with status {}", cycle.getId(), cycle.getStatus());

        } catch (Exception e) {
            log.error("Cycle {}: Execution failed: {}", cycle.getId(), e.getMessage(), e);

            cycle.setStatus(CycleStatusEnum.FAILED);
            cycle.setCompletedAt(new Date());
            cycle.setErrorMessage("Execution failed: " + e.getMessage());
            cycleRepository.save(cycle);

            throw new ExecutionException("Cycle execution failed: " + e.getMessage(), e);
        }
    }

    private boolean isMssqlTarget(Cycle cycle) {
        return cycle.getTargetConnector().getDatabaseType().name().equals("MSSQL");
    }

    private void updateCycleStatus(Cycle cycle) {
        List<Task> tasks = taskRepository.findByCycle_Id(cycle.getId());
        long failedCount = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatusEnum.FAILED)
                .count();

        if (failedCount > 0) {
            cycle.setStatus(CycleStatusEnum.FAILED);
            cycle.setErrorMessage(failedCount + " task(s) failed. Check task logs for details.");
        } else {
            cycle.setStatus(CycleStatusEnum.COMPLETED);
        }

        cycle.setCompletedAt(new Date());
        cycleRepository.save(cycle);
    }
}
