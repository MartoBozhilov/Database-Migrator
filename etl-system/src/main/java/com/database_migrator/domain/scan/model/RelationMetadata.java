package com.database_migrator.domain.scan.model;

import com.database_migrator.domain.common.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Immutable
@Table(name = "relations_metadata")
public class RelationMetadata extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "system_scan_id", nullable = false, updatable = false)
    private SystemScan systemScan;

    @NotNull
    @Column(name = "foreign_table", updatable = false)
    private String foreignTable;

    @NotNull
    @Column(name = "foreign_column", nullable = false, updatable = false)
    private String foreignColumn;

    @NotNull
    @Column(name = "primary_table", updatable = false)
    private String primaryTable;

    @Column(name = "primary_column", nullable = false, updatable = false)
    private String primaryColumn;
}
