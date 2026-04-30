package com.db_migrator.etl_system.model.entity.metadata;

import com.db_migrator.etl_system.model.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Immutable
@Table(
        name = "tables_metadata",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"system_scan_id", "table_name"}
        )
)
public class TableMetadata extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "system_scan_id", nullable = false)
    private SystemScan systemScan;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @OneToMany(mappedBy = "table")
    private List<ColumnMetadata> columnMetadataList = new ArrayList<>();
}
