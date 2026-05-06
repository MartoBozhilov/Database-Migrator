package com.database_migrator.domain.scan.model;

import com.database_migrator.domain.common.model.BaseEntity;
import com.database_migrator.domain.connector.model.Connector;
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
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Immutable
@Table(name = "system_scans")
public class SystemScan extends BaseEntity {

    @NotNull
    @Column(nullable = false, updatable = false)
    private String name;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @Column(name = "created_at", updatable = false)
    private Date createdAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_connector", nullable = false, updatable = false)
    private Connector sourceConnector;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, updatable = false)
    private ScanStatusEnum status;

    @Column(name = "started_at", updatable = false)
    private Date startedAt;

    @Column(name = "completed_at", updatable = false)
    private Date completedAt;

    @Column(name = "error_message", updatable = false, columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "systemScan", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TableMetadata> tableMetadataList = new ArrayList<>();

    @OneToMany(mappedBy = "systemScan", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RelationMetadata> relationMetadataList = new ArrayList<>();
}
