package com.db_migrator.etl_system.model.entity.metadata;

import com.db_migrator.etl_system.model.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Immutable
@Table(name = "relations_metadata")
public class RelationMetadata extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "system_scan_id", nullable = false)
    private SystemScan systemScan;

    @NotNull
    @Column(name = "foreign_table")
    private String foreignTable;

    @NotNull
    @Column(name = "foreign_column", nullable = false)
    private String foreignColumn;

    @NotNull
    @Column(name = "primary_table")
    private String primaryTable;

    @Column(name = "primary_column", nullable = false)
    private String primaryColumn;
}
