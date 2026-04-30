package com.db_migrator.etl_system.model.entity.metadata;

import com.db_migrator.etl_system.model.entity.BaseEntity;
import com.db_migrator.etl_system.model.entity.connector.Connector;
import com.db_migrator.etl_system.model.entity.user.User;

import com.db_migrator.etl_system.model.enums.ScanStatusEnum;
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
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Immutable
@Table(
        name = "system_scans",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_system_scan_name_organization",
                columnNames = {"name", "created_by"}
        )
)
public class SystemScan extends BaseEntity {

    @NotNull
    @Column
    private String name;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @NotNull
    @Column(name = "created_at")
    private Date createdAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_connector", nullable = false)
    private Connector sourceConnector;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScanStatusEnum status = ScanStatusEnum.RUNNING;

    @Column(name = "started_at")
    private Date startedAt;

    @Column(name = "completed_at")
    private Date completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "systemScan")
    private List<TableMetadata> tableMetadataList = new ArrayList<>();

    @OneToMany(mappedBy = "systemScan")
    private List<RelationMetadata> relationMetadataList = new ArrayList<>();
}
