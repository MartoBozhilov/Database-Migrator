package com.db_migrator.etl_system.model.entity.transformation;

import com.db_migrator.etl_system.model.entity.BaseEntity;
import com.db_migrator.etl_system.model.entity.metadata.RelationMetadata;
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

    // Changes of relations (here we apply changes/updates on relations on assignments)
    // ADD_RELATION -> creates new entry -> (sourceRelationMetadata == null)
    // DELETE_RELATION ->  isDeleted flag

    // RENAME_RELATION -> not user triggered (system (java logic) handled) when
    // rename table or column that is in relationship
    // example user_id is fk in table orders -> user rename user_id to system_user_id -> we should handle to
    // update relation entity fk from user_id to system_user_id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformation_model_id", nullable = false)
    private TransformationModel transformationModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_relation_metadata_id")
    private RelationMetadata sourceRelationMetadata;  // NULL for ADD_RELATION

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
