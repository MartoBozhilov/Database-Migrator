package com.db_migrator.etl_system.service;

import com.db_migrator.etl_system.dto.helper.ValidationResult;
import com.db_migrator.etl_system.dto.request.CycleCreateRequest;
import com.db_migrator.etl_system.dto.response.CycleDetailsResponse;
import com.db_migrator.etl_system.dto.response.CycleResponse;
import com.db_migrator.etl_system.mapper.ResponseMapper;
import com.db_migrator.etl_system.model.entity.execution.Cycle;
import com.db_migrator.etl_system.model.entity.execution.Task;
import com.db_migrator.etl_system.model.entity.transformation.TransformationModel;
import com.db_migrator.etl_system.model.entity.transformation.TransformationTable;
import com.db_migrator.etl_system.model.entity.user.User;
import com.db_migrator.etl_system.model.enums.CycleStatusEnum;
import com.db_migrator.etl_system.model.enums.TaskStatusEnum;
import com.db_migrator.etl_system.repository.CycleRepository;
import com.db_migrator.etl_system.repository.TaskRepository;
import com.db_migrator.etl_system.repository.TransformationModelRepository;
import com.db_migrator.etl_system.security.SecurityUtils;
import com.db_migrator.etl_system.service.execution.AsyncCycleExecutor;
import com.db_migrator.etl_system.service.execution.DAGBuilder;
import com.db_migrator.etl_system.service.execution.PreExecutionValidator;
import com.db_migrator.etl_system.service.transformation.TransformationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CycleService {

    private final TransformationModelRepository transformationModelRepository;
    private final CycleRepository cycleRepository;
    private final TaskRepository taskRepository;
    private final SecurityUtils securityUtils;
    private final ResponseMapper responseMapper;

    // Execution services
    private final PreExecutionValidator preExecutionValidator;
    private final DAGBuilder dagBuilder;
    private final AsyncCycleExecutor asyncCycleExecutor;

    @Transactional
    public CycleResponse createCycle(CycleCreateRequest request) {
        Long orgId = securityUtils.getCurrentOrganizationId();
        User currentUser = securityUtils.getCurrentUser();

        log.info("Creating cycle '{}' for organization {}", request.getName(), orgId);

        if (cycleRepository.existsByNameAndCreatedBy_Organization_Id(request.getName(), orgId)) {
            throw new RuntimeException("Cycle with name '" + request.getName() + "' already exists");
        }

        TransformationModel model = transformationModelRepository
                .findByIdAndCreatedBy_Organization_Id(request.getTransformationModelId(), orgId)
                .orElseThrow(() -> new RuntimeException("Transformation model not found"));

        if (!model.getIsConfirmed()) {
            throw new RuntimeException("Transformation model must be confirmed before creating a cycle");
        }

        log.info("Building cycle entity");
        Cycle cycle = buildCycle(request, model, currentUser);
        cycle = cycleRepository.save(cycle);
        log.info("Cycle {} saved to database", cycle.getId());

        try {
            log.info("Creating tasks from DAG for cycle {}", cycle.getId());
            createTasksFromDAG(cycle, model);
            log.info("Tasks created successfully for cycle {}", cycle.getId());
        } catch (Exception e) {
            log.error("Failed to create tasks from DAG for cycle {}: {}", cycle.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create tasks: " + e.getMessage(), e);
        }

        log.info("Cycle {} created with {} tasks", cycle.getId(), cycle.getTasks().size());
        return responseMapper.toCycleResponse(cycle);
    }

    public ValidationResult validateCycle(Long cycleId) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        Cycle cycle = cycleRepository.findByIdAndCreatedBy_Organization_Id(cycleId, orgId)
                .orElseThrow(() -> new RuntimeException("Cycle not found"));

        return preExecutionValidator.validateCycle(cycle);
    }

    @Transactional
    public void executeCycle(Long cycleId) {
        Long orgId = securityUtils.getCurrentOrganizationId();

        Cycle cycle = cycleRepository.findByIdAndCreatedBy_Organization_Id(cycleId, orgId)
                .orElseThrow(() -> new RuntimeException("Cycle not found"));

        if (cycle.getStatus() != CycleStatusEnum.CREATED) {
            throw new RuntimeException("Cycle must be in CREATED status to execute (current: " + cycle.getStatus() + ")");
        }

        log.info("Cycle {}: Validating and starting execution", cycle.getId());

        ValidationResult validation = preExecutionValidator.validateCycle(cycle);
        if (validation.hasErrors()) {
            cycle.setStatus(CycleStatusEnum.FAILED);
            cycle.setErrorMessage("Pre-execution validation failed: " + validation.getErrorMessages());
            cycleRepository.save(cycle);

            throw new RuntimeException("Pre-execution validation failed: " + validation.getErrorMessages());
        }

        // Mark cycle as running
        cycle.setStatus(CycleStatusEnum.RUNNING);
        cycle.setStartedAt(new Date());
        cycleRepository.save(cycle);

        log.info("Cycle {}: Marked as RUNNING, triggering async execution", cycle.getId());

        // Trigger async execution
        asyncCycleExecutor.executeAsync(cycleId);
    }

    public List<CycleResponse> findAll() {
        Long orgId = securityUtils.getCurrentOrganizationId();
        return cycleRepository.findByCreatedBy_Organization_Id(orgId).stream()
                .map(responseMapper::toCycleResponse)
                .toList();
    }

    public CycleResponse findById(Long id) {
        Long orgId = securityUtils.getCurrentOrganizationId();
        Cycle cycle = cycleRepository.findByIdAndCreatedBy_Organization_Id(id, orgId)
                .orElseThrow(() -> new RuntimeException("Cycle not found"));
        return responseMapper.toCycleResponse(cycle);
    }

    public CycleDetailsResponse getDetails(Long id) {
        Long orgId = securityUtils.getCurrentOrganizationId();
        Cycle cycle = cycleRepository.findByIdAndCreatedBy_Organization_Id(id, orgId)
                .orElseThrow(() -> new RuntimeException("Cycle not found"));
        return responseMapper.toCycleDetailsResponse(cycle);
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = securityUtils.getCurrentOrganizationId();
        Cycle cycle = cycleRepository.findByIdAndCreatedBy_Organization_Id(id, orgId)
                .orElseThrow(() -> new RuntimeException("Cycle not found"));

        if (cycle.getStatus() == CycleStatusEnum.RUNNING) {
            throw new RuntimeException("Cannot delete running cycle");
        }

        cycleRepository.delete(cycle);
        log.info("Cycle {} deleted", id);
    }

    private Cycle buildCycle(CycleCreateRequest request, TransformationModel model, User currentUser) {
        Cycle cycle = new Cycle();
        cycle.setName(request.getName());
        cycle.setTransformationModel(model);
        cycle.setSourceConnector(model.getSystemScan().getSourceConnector());
        cycle.setTargetConnector(model.getTargetConnector());
        cycle.setStatus(CycleStatusEnum.CREATED);
        cycle.setCreatedBy(currentUser);
        cycle.setCreatedAt(new Date());
        return cycle;
    }

    private void createTasksFromDAG(Cycle cycle, TransformationModel model) {
        // Build dependency graph
        Map<String, Set<String>> graph = dagBuilder.buildDependencyGraph(model);

        // Build map: tableName -> TransformationTable
        Map<String, TransformationTable> tableMap = getTransformationTableByNameMap(model);

        // Build map: tableName -> new Created tasks for table
        Map<String, Task> taskMap = buildTableNameTaskMap(cycle, graph, tableMap);

        // Set dependencies
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            String tableName = entry.getKey();
            Set<String> dependencies = entry.getValue();

            Task task = taskMap.get(tableName);
            if (task == null) continue;

            Set<Task> dependencyTasks = findDependencyTasks(dependencies, taskMap);

            task.setDependencies(dependencyTasks);
        }

        // Save tasks
        List<Task> tasks = new ArrayList<>(taskMap.values());
        taskRepository.saveAll(tasks);

        cycle.setTasks(tasks);
    }

    private Set<Task> findDependencyTasks(Set<String> dependencies, Map<String, Task> taskMap) {
        Set<Task> dependencyTasks = new HashSet<>();

        for (String depTableName : dependencies) {
            Task depTask = taskMap.get(depTableName);
            if (depTask != null) {
                dependencyTasks.add(depTask);
            }
        }
        return dependencyTasks;
    }

    private Map<String, Task> buildTableNameTaskMap(Cycle cycle, Map<String, Set<String>> graph, Map<String, TransformationTable> tableMap) {
        Map<String, Task> taskMap = new HashMap<>();

        for (String tableName : graph.keySet()) {
            TransformationTable transformationTable = tableMap.get(tableName);
            if (transformationTable == null) {
                log.warn("TransformationTable not found for table: {}", tableName);
                continue;
            }

            Task task = buildTask(cycle, transformationTable);
            taskMap.put(tableName, task);
        }
        return taskMap;
    }

    private Map<String, TransformationTable> getTransformationTableByNameMap(TransformationModel model) {
        Map<String, TransformationTable> tableMap = new HashMap<>();
        for (TransformationTable table : model.getTransformationTables()) {
            if (!TransformationUtils.isTableDeleted(table)) {
                String effectiveName = TransformationUtils.getEffectiveTableName(table);
                if (effectiveName != null) {
                    tableMap.put(effectiveName, table);
                }
            }
        }
        return tableMap;
    }

    private Task buildTask(Cycle cycle, TransformationTable transformationTable) {
        Task task = new Task();
        task.setCycle(cycle);
        task.setTransformationTable(transformationTable);
        task.setStatus(TaskStatusEnum.PENDING);
        task.setRowsProcessed(0L);
        return task;
    }
}
