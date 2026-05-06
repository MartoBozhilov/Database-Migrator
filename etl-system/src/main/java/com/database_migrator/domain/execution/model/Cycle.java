package com.database_migrator.domain.execution.model;

import com.database_migrator.domain.common.model.BaseEntity;
import com.database_migrator.domain.connector.model.Connector;
import com.database_migrator.domain.transformation.model.TransformationModel;
import com.database_migrator.domain.auth.model.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "cycles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cycle_name_organization",
                columnNames = {"name", "created_by_id"}
        )
)
public class Cycle extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Date createdAt = new Date();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformation_model_id", nullable = false)
    private TransformationModel transformationModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_connector_id", nullable = false)
    private Connector sourceConnector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_connector_id", nullable = false)
    private Connector targetConnector;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CycleStatusEnum status = CycleStatusEnum.CREATED;

    @Column(name = "started_at")
    private Date startedAt;

    @Column(name = "completed_at")
    private Date completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "cycle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();
}
