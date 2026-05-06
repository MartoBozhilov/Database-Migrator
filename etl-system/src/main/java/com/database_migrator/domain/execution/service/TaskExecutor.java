package com.database_migrator.domain.execution.service;

import com.database_migrator.domain.execution.model.Task;
import com.database_migrator.domain.transformation.model.TransformationModel;
import com.database_migrator.domain.transformation.model.TransformationTable;
import com.database_migrator.domain.connector.model.DatabaseTypeEnum;
import com.database_migrator.domain.execution.model.TaskStatusEnum;
import com.database_migrator.domain.execution.repository.TaskRepository;
import com.database_migrator.domain.transformation.repository.TransformationModelRepository;
import com.database_migrator.domain.transformation.repository.TransformationTableRepository;
import com.database_migrator.domain.common.util.TransformationUtils;
import com.database_migrator.domain.common.util.DatabaseConnectionManager;
import com.database_migrator.domain.common.util.ConnectionPair;
import com.database_migrator.domain.common.exception.ResourceNotFoundException;
import com.database_migrator.domain.common.exception.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Statement;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskExecutor {

    private final DDLGenerator ddlGenerator;
    private final DataMigrationService dataMigrationService;
    private final TransformationModelRepository transformationModelRepository;
    private final TransformationTableRepository transformationTableRepository;
    private final TaskRepository taskRepository;
    private final DatabaseConnectionManager connectionManager;

    @Transactional
    public void executeTaskById(Long taskId) {
        // Fetch task fresh with all associations
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        TransformationTable table = transformationTableRepository.findById(task.getTransformationTable().getId())
                .orElseThrow(() -> new ResourceNotFoundException("TransformationTable", task.getTransformationTable().getId()));

        String tableName = TransformationUtils.getEffectiveTableName(table);

        log.info("Task {}: Starting execution for table {}", task.getId(), tableName);

        task.setStatus(TaskStatusEnum.RUNNING);
        task.setStartedAt(new Date());
        task.setRowsProcessed(0L);
        taskRepository.save(task);

        try {
            Long modelId = table.getTransformationModel().getId();
            TransformationModel model = transformationModelRepository.findByIdWithAssociations(modelId)
                    .orElseThrow(() -> new ResourceNotFoundException("TransformationModel", modelId));

            try (ConnectionPair connections = connectionManager.createConnectionPair(
                    model.getSystemScan().getSourceConnector(),
                    model.getTargetConnector())) {

                // Step 1: CREATE TABLE with FK constraints
                log.info("Task {}: Creating table {} with FK constraints", task.getId(), tableName);

                DatabaseTypeEnum targetDb = model.getTargetConnector().getDatabaseType();
                String createTableDDL = ddlGenerator.generateCreateTableDDL(table, model, targetDb);

                try (Statement stmt = connections.target().createStatement()) {
                    stmt.execute(createTableDDL);
                    connections.target().commit();
                }

                log.info("Task {}: Table {} created successfully", task.getId(), tableName);

                // Step 2: MIGRATE DATA (batched with transformations)
                log.info("Task {}: Migrating data for table {}", task.getId(), tableName);

                long rowsMigrated = dataMigrationService.migrateTableData(
                        task,
                        connections.source(),
                        connections.target(),
                        table);

                log.info("Task {}: Data migration completed. Rows: {}", task.getId(), rowsMigrated);

                // Mark task as completed
                task.setStatus(TaskStatusEnum.COMPLETED);
                task.setCompletedAt(new Date());
                task.setRowsProcessed(rowsMigrated);
                taskRepository.save(task);

                log.info("Task {}: Execution completed successfully for table {} ({} rows)",
                        task.getId(), tableName, rowsMigrated);

            }

        } catch (ResourceNotFoundException e) {
            // Re-throw ResourceNotFoundException as-is
            throw e;
        } catch (ExecutionException e) {
            log.error("Task {}: Execution failed for table {}: {}",
                    task.getId(), tableName, e.getMessage(), e);

            task.setStatus(TaskStatusEnum.FAILED);
            task.setCompletedAt(new Date());
            task.setErrorMessage(e.getMessage());
            taskRepository.save(task);

            throw e;
        } catch (Exception e) {
            log.error("Task {}: Unexpected failure for table {}: {}",
                    task.getId(), tableName, e.getMessage(), e);

            task.setStatus(TaskStatusEnum.FAILED);
            task.setCompletedAt(new Date());
            task.setErrorMessage(e.getMessage());
            taskRepository.save(task);

            throw new ExecutionException("Task execution failed: " + e.getMessage(), e);
        }
    }
}
