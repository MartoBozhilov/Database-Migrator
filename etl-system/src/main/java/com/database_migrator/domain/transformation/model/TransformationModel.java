package com.database_migrator.domain.transformation.model;

import com.database_migrator.domain.common.model.BaseEntity;
import com.database_migrator.domain.connector.model.Connector;
import com.database_migrator.domain.scan.model.SystemScan;
import com.database_migrator.domain.auth.model.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "transformation_models")
public class TransformationModel extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @NotNull
    @Column(name = "created_at")
    private Date createdAt = new Date();

    @Column(name = "is_confirmed", nullable = false)
    private Boolean isConfirmed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_scan_id", nullable = false, updatable = false)
    private SystemScan systemScan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_connector_id", updatable = false)
    private Connector targetConnector;

    @OneToMany(mappedBy = "transformationModel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransformationTable> transformationTables = new ArrayList<>();

    @OneToMany(mappedBy = "transformationModel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransformationRelation> transformationRelations = new ArrayList<>();
}
