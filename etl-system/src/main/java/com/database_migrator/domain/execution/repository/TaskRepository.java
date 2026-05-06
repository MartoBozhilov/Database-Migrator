package com.database_migrator.domain.execution.repository;

import com.database_migrator.domain.execution.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByCycle_Id(Long cycleId);
}
