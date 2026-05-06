package com.database_migrator.domain.execution.model;

import com.database_migrator.domain.common.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "task_logs")
public class TaskLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "timestamp", nullable = false)
    private Date timestamp = new Date();

    @Column(name = "level", nullable = false, length = 10)
    private String level; // INFO, WARN, ERROR

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
}
