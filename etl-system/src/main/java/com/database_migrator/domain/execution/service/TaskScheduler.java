package com.database_migrator.domain.execution.service;

import com.database_migrator.domain.common.util.TransformationUtils;
import com.database_migrator.domain.execution.model.Cycle;
import com.database_migrator.domain.execution.model.Task;
import com.database_migrator.domain.common.exception.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskScheduler {

    public Map<Task, Set<Task>> buildTaskGraph(Cycle cycle) {
        Map<Task, Set<Task>> graph = new HashMap<>();

        // Initialize all tasks in graph
        for (Task task : cycle.getTasks()) {
            graph.put(task, new HashSet<>(task.getDependencies()));
        }

        log.info("Built task graph with {} tasks", graph.size());

        return graph;
    }

    /**
     * Compute execution batches using topological sort
     * Algorithm:
     * 1. Start with tasks that have 0 dependencies (batch 0)
     * 2. Remove those tasks from the graph
     * 3. Find tasks whose dependencies are now all satisfied (batch 1)
     * 4. Repeat until all tasks are scheduled
     *
     * @return Deque of task batches ordered by execution level (FIFO order)
     */
    public Deque<List<Task>> computeExecutionBatches(Map<Task, Set<Task>> taskGraph) {
        List<List<Task>> batches = new ArrayList<>();

        // Create mutable copy of dependency sets
        Map<Task, Set<Task>> remainingDeps = new HashMap<>();
        for (Map.Entry<Task, Set<Task>> entry : taskGraph.entrySet()) {
            remainingDeps.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        Set<Task> completedTasks = new HashSet<>();

        // Iteratively find tasks whose dependencies are all completed
        while (!remainingDeps.isEmpty()) {
            // Find all tasks with no remaining dependencies
            List<Task> currentBatch = remainingDeps.entrySet().stream()
                    .filter(entry -> entry.getValue().isEmpty())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (currentBatch.isEmpty()) {
                // Circular dependency detected
                log.error("Circular dependency detected! Remaining tasks: {}",
                        remainingDeps.keySet().stream()
                                .map(task -> TransformationUtils
                                        .getEffectiveTableName(task.getTransformationTable()))
                                .collect(Collectors.joining(", ")));
                throw new ExecutionException("Circular dependency detected in task graph", null);
            }

            batches.add(currentBatch);
            completedTasks.addAll(currentBatch);

            // Remove completed tasks from the graph
            for (Task completed : currentBatch) {
                remainingDeps.remove(completed);
            }

            // Remove completed tasks from remaining dependencies
            for (Set<Task> deps : remainingDeps.values()) {
                deps.removeAll(completedTasks);
            }
        }

        log.info("Computed {} execution batches from {} tasks using topological sort",
                batches.size(), taskGraph.size());

        logTaskBatchesDetails(batches);

        return new ArrayDeque<>(batches);
    }

    private void logTaskBatchesDetails(List<List<Task>> batches) {
        for (int i = 0; i < batches.size(); i++) {
            List<Task> batch = batches.get(i);
            log.debug("Batch {}: {} tasks: {}",
                    i, batch.size(),
                    batch.stream()
                            .map(task -> TransformationUtils
                                    .getEffectiveTableName(task.getTransformationTable()))
                            .collect(Collectors.joining(", ")));
        }
    }
}
