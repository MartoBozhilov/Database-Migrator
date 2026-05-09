package com.database_migrator.domain.transformation.dto;

import com.database_migrator.domain.connector.model.DatabaseTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransformationModelResponse {
    private Long id;
    private String name;
    private Boolean isConfirmed;
    private Long systemScanId;
    private String systemScanName;
    private Long sourceConnectorId;
    private String sourceConnectorName;
    private DatabaseTypeEnum sourceDatabaseType;
    private Long targetConnectorId;
    private String targetConnectorName;
    private DatabaseTypeEnum targetDatabaseType;
    private Long createdById;
    private String createdByName;
    private Date createdAt;
    private Integer tableCount;
    private Integer columnCount;
    private Integer relationCount;
}
