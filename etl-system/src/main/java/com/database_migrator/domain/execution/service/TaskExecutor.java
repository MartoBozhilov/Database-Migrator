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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Date;

/**
 * Task Executor Service
 * Orchestrates execution flow (ONE-STEP APPROACH):
 * 1. CREATE TABLE with FK constraints (DDL)
 * 2. MIGRATE DATA (batched) - FK constraints validate immediately
 * 3. Track progress
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskExecutor {

    private final DDLGenerator ddlGenerator;
    private final DataMigrationService dataMigrationService;
    private final TransformationModelRepository transformationModelRepository;
    private final TransformationTableRepository transformationTableRepository;
    private final TaskRepository taskRepository;

    /**
     * Execute a single task by ID (create table + migrate data)
     * IMPORTANT: This method runs in a background thread and must manage its own transaction.
     * It fetches all data fresh to avoid Hibernate session issues.
     *
     * @param taskId Task ID to execute
     */
    @Transactional
    public void executeTaskById(Long taskId) {
        // Fetch task fresh with all associations
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        // Fetch transformation table with all associations
        TransformationTable table = transformationTableRepository.findById(task.getTransformationTable().getId())
                .orElseThrow(() -> new RuntimeException("TransformationTable not found"));

        String tableName = TransformationUtils.getEffectiveTableName(table);

        log.info("Task {}: Starting execution for table {}", task.getId(), tableName);

        task.setStatus(TaskStatusEnum.RUNNING);
        task.setStartedAt(new Date());
        task.setRowsProcessed(0L);
        taskRepository.save(task);

        try {
            // Fetch transformation model with all associations eagerly loaded
            Long modelId = table.getTransformationModel().getId();
            TransformationModel model = transformationModelRepository.findByIdWithAssociations(modelId)
                    .orElseThrow(() -> new RuntimeException("Transformation model not found: " + modelId));

            // Get database connections
            String sourceJdbcUrl = buildJdbcUrl(
                model.getSystemScan().getSourceConnector().getHost(),
                model.getSystemScan().getSourceConnector().getPort(),
                model.getSystemScan().getSourceConnector().getDatabaseName(),
                model.getSystemScan().getSourceConnector().getDatabaseType()
            );

            String targetJdbcUrl = buildJdbcUrl(
                model.getTargetConnector().getHost(),
                model.getTargetConnector().getPort(),
                model.getTargetConnector().getDatabaseName(),
                model.getTargetConnector().getDatabaseType()
            );

            try (Connection sourceConn = DriverManager.getConnection(
                    sourceJdbcUrl,
                    model.getSystemScan().getSourceConnector().getUsername(),
                    model.getSystemScan().getSourceConnector().getPassword());

                 Connection targetConn = DriverManager.getConnection(
                    targetJdbcUrl,
                    model.getTargetConnector().getUsername(),
                    model.getTargetConnector().getPassword())) {

                // Disable auto-commit for batch operations
                targetConn.setAutoCommit(false);
                sourceConn.setAutoCommit(false);

                // Step 1: CREATE TABLE with FK constraints (ONE-STEP APPROACH)
                log.info("Task {}: Creating table {} with FK constraints", task.getId(), tableName);

                DatabaseTypeEnum targetDb = model.getTargetConnector().getDatabaseType();
                String createTableDDL = ddlGenerator.generateCreateTableDDL(table, model, targetDb);

                try (Statement stmt = targetConn.createStatement()) {
                    stmt.execute(createTableDDL);
                    targetConn.commit();
                }

                log.info("Task {}: Table {} created successfully", task.getId(), tableName);

                // Step 2: MIGRATE DATA (batched with transformations)
                log.info("Task {}: Migrating data for table {}", task.getId(), tableName);

                long rowsMigrated = dataMigrationService.migrateTableData(task, sourceConn, targetConn, table);

                log.info("Task {}: Data migration completed. Rows: {}", task.getId(), rowsMigrated);

                // Mark task as completed
                task.setStatus(TaskStatusEnum.COMPLETED);
                task.setCompletedAt(new Date());
                task.setRowsProcessed(rowsMigrated);
                taskRepository.save(task);

                log.info("Task {}: Execution completed successfully for table {} ({} rows)",
                    task.getId(), tableName, rowsMigrated);

            }

        } catch (Exception e) {
            // Handle failure
            log.error("Task {}: Execution failed for table {}: {}",
                task.getId(), tableName, e.getMessage(), e);

            task.setStatus(TaskStatusEnum.FAILED);
            task.setCompletedAt(new Date());
            task.setErrorMessage(e.getMessage());
            taskRepository.save(task);

            throw new RuntimeException("Task execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build JDBC URL
     */
    private String buildJdbcUrl(String host, Integer port, String database, DatabaseTypeEnum dbType) {
        return String.format("jdbc:%s://%s:%d/%s",
            dbType.name().toLowerCase(),
            host,
            port,
            database);
    }
}
