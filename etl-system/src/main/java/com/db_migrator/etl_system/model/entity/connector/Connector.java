package com.db_migrator.etl_system.model.entity.connector;

import com.db_migrator.etl_system.model.entity.BaseEntity;
import com.db_migrator.etl_system.model.entity.user.User;

import com.db_migrator.etl_system.model.enums.ConnectorTypeEnum;
import com.db_migrator.etl_system.model.enums.DatabaseTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "connectors",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_connector_name_organization",
                columnNames = {"name", "created_by"}
        )
)
public class Connector extends BaseEntity {

    @NotNull
    @Column
    private String name;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @NotNull
    @Column(name = "created_at")
    private Date createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "database_type")
    private DatabaseTypeEnum databaseType;

    @NotNull
    @Column
    private String host;

    @NotNull
    @Column
    private Integer port;

    @NotNull
    @Column(name = "database_name")
    private String databaseName;

    @NotNull
    @Column
    private String username;

    @NotNull
    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "connector_type")
    private ConnectorTypeEnum connectorType;
}
