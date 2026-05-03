package com.db_migrator.etl_system.model.entity.transformation;

import com.db_migrator.etl_system.model.entity.BaseEntity;
import com.db_migrator.etl_system.model.entity.metadata.TableMetadata;

import jakarta.persistence.CascadeType;
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

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transformation_tables")
public class TransformationTable extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformation_model_id", nullable = false)
    private TransformationModel transformationModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_table_metadata_id")
    private TableMetadata sourceTableMetadata;  // NULL for ADD_TABLE

    @OneToMany(mappedBy = "transformationTable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransformationColumn> columns = new ArrayList<>();

    @OneToMany(mappedBy = "transformationTable", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TableTransformationAssignment> assignments = new ArrayList<>();
}
