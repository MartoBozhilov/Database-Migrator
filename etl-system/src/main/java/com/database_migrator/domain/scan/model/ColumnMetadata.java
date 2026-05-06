package com.database_migrator.domain.scan.model;

import com.database_migrator.domain.common.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "columns_metadata")
public class ColumnMetadata extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "table_metadata_id", nullable = false, updatable = false)
    private TableMetadata table;

    @Column(name = "column_name", nullable = false, updatable = false)
    private String columnName;

    @Column(name = "data_type", nullable = false, updatable = false)
    private String dataType;

    @Column(name = "is_nullable", nullable = false, updatable = false)
    private Boolean isNullable;

    @Column(name = "length", updatable = false)
    private Integer length;

    @Column(name = "is_primary_key", nullable = false, updatable = false)
    private Boolean isPrimaryKey;

    @Column(name = "is_auto_increment", nullable = false, updatable = false)
    private Boolean isAutoIncrement;
}
