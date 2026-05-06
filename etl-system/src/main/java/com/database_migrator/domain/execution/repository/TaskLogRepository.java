package com.database_migrator.domain.execution.repository;

import com.database_migrator.domain.execution.model.TaskLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskLogRepository extends JpaRepository<TaskLog, Long> {
}
