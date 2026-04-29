package com.db_migrator.etl_system.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transformation_table_filters")
public class TransformationTableFilter extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "transformation_table_id")
    private TransformationTable table;


}
