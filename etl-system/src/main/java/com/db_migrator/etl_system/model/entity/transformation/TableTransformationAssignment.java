package com.db_migrator.etl_system.model.entity.transformation;

import com.db_migrator.etl_system.model.entity.BaseEntity;
import com.db_migrator.etl_system.model.enums.TableTransformationType;

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
@Table(name = "table_transformation_assignments")
public class TableTransformationAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformation_table_id", nullable = false)
    private TransformationTable transformationTable;

    @Enumerated(EnumType.STRING)
    @Column(name = "transformation_type", nullable = false, length = 50)
    private TableTransformationType transformationType;

    // Parameter for RENAME_TABLE
    @Column(name = "new_name")
    private String newName;

    @Column(name = "created_at", nullable = false)
    private Date createdAt = new Date();
}
