package com.db_migrator.etl_system.dto.response;

import com.db_migrator.etl_system.model.enums.DatabaseTypeEnum;
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
    private Long systemScanId;
    private String systemScanName;
    private Long targetConnectorId;
    private String targetConnectorName;
    private DatabaseTypeEnum targetDatabaseType;
    private Long createdById;
    private String createdByName;
    private Date createdAt;
    private Integer tableCount;
    private Integer columnCount;
}
