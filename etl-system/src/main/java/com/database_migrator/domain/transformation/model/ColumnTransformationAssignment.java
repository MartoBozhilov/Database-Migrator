package com.database_migrator.domain.transformation.model;

import com.database_migrator.domain.common.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "column_transformation_assignments")
public class ColumnTransformationAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformation_column_id", nullable = false)
    private TransformationColumn transformationColumn;

    @Enumerated(EnumType.STRING)
    @Column(name = "transformation_type", nullable = false, length = 50)
    private ColumnTransformationType transformationType;

    // For RENAME_COLUMN
    @Column(name = "new_name", length = 64)
    private String newName;

    // For CHANGE_TYPE
    @Column(name = "target_data_type", length = 100)
    private String targetDataType;

    // For ADD_COLUMN (user-created column definition)
    @Column(name = "column_name", length = 64)
    private String columnName;

    @Column(name = "data_type", length = 100)
    private String dataType;

    @Column(name = "is_nullable")
    private Boolean isNullable;

    @Column(name = "is_primary_key")
    private Boolean isPrimaryKey;

    @Column(name = "default_value", length = 500)
    private String defaultValue;
}
