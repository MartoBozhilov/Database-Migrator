package com.db_migrator.etl_system.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "columns_metadata",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"table_metadata_id", "column_name"}
        )
)
public class ColumnMetadata extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "table_metadata_id", nullable = false)
    private TableMetadata table;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    @Column(name = "is_nullable", nullable = false)
    private Boolean isNullable;

    @Column(name = "length")
    private Integer length;
}
