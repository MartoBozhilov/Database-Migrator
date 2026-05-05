package com.db_migrator.etl_system.repository;

import com.db_migrator.etl_system.model.entity.execution.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByCycle_Id(Long cycleId);
}
