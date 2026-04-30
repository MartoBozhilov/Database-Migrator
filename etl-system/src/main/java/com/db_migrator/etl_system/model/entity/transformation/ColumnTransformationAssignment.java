package com.db_migrator.etl_system.model.entity.transformation;

import com.db_migrator.etl_system.model.entity.BaseEntity;
import com.db_migrator.etl_system.model.enums.ColumnTransformationType;

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

import java.util.Date;

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

    @Column(name = "created_at", nullable = false)
    private Date createdAt = new Date();

    // For RENAME_COLUMN
    @Column(name = "new_name")
    private String newName;

    // For CHANGE_TYPE
    @Column(name = "target_data_type", length = 100)
    private String targetDataType;

    // For EXPRESSION
    @Column(name = "expression", columnDefinition = "TEXT")
    private String expression;

    // For DEFAULT_VALUE
    @Column(name = "default_value")
    private String defaultValue;
}
