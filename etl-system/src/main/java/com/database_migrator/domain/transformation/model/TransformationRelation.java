package com.database_migrator.domain.transformation.model;

import com.database_migrator.domain.common.model.BaseEntity;
import com.database_migrator.domain.scan.model.RelationMetadata;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transformation_relations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransformationRelation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformation_model_id", nullable = false)
    private TransformationModel transformationModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_relation_metadata_id")
    private RelationMetadata sourceRelationMetadata;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @NotNull
    @Column(name = "foreign_table", nullable = false)
    private String foreignTable;

    @NotNull
    @Column(name = "foreign_column", nullable = false)
    private String foreignColumn;

    @NotNull
    @Column(name = "primary_table", nullable = false)
    private String primaryTable;

    @NotNull
    @Column(name = "primary_column", nullable = false)
    private String primaryColumn;
}
