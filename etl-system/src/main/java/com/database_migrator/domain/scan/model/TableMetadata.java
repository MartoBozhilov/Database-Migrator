package com.database_migrator.domain.scan.model;

import com.database_migrator.domain.common.model.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Immutable
@Table(name = "tables_metadata")
public class TableMetadata extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "system_scan_id", nullable = false, updatable = false)
    private SystemScan systemScan;

    @Column(name = "table_name", nullable = false, updatable = false)
    private String tableName;

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ColumnMetadata> columnMetadataList = new ArrayList<>();
}
