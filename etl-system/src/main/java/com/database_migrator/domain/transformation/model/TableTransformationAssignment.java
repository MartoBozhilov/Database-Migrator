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
@Table(name = "table_transformation_assignments")
public class TableTransformationAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformation_table_id", nullable = false)
    private TransformationTable transformationTable;

    @Enumerated(EnumType.STRING)
    @Column(name = "transformation_type", nullable = false, length = 50)
    private TableTransformationType transformationType;

    // For RENAME_TABLE
    @Column(name = "new_name", length = 64)
    private String newName;

    // For ADD_TABLE (user-created table definition)
    @Column(name = "table_name", length = 64)
    private String tableName;

    @Column(name = "id_generation_strategy", length = 50)
    private String idGenerationStrategy;  // AUTO_INCREMENT, UUID, SEQUENCE
}
