package com.db_migrator.etl_system.model.entity.transformation;

import com.db_migrator.etl_system.model.entity.BaseEntity;
import com.db_migrator.etl_system.model.entity.metadata.ColumnMetadata;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transformation_columns")
public class TransformationColumn extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformation_table_id", nullable = false)
    private TransformationTable transformationTable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_column_metadata_id")
    private ColumnMetadata sourceColumnMetadata;

    // Resolved target database type
    @Column(name = "resolved_target_type", length = 100)
    private String resolvedTargetType;

    @OneToMany(mappedBy = "transformationColumn", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ColumnTransformationAssignment> assignments = new ArrayList<>();
}
