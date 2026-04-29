package com.db_migrator.etl_system.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transformation_tables")
public class TransformationTable extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "transformation_model_id")
    private TransformationModel model;

    @Column(name = "table_name", nullable = false)
    private String tableName;



}
