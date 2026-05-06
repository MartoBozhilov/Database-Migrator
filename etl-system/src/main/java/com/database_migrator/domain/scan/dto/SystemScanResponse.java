package com.database_migrator.domain.scan.dto;

import com.database_migrator.domain.scan.model.ScanStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemScanResponse {

    private Long id;
    private String name;
    private Long sourceConnectorId;
    private String sourceConnectorName;
    private ScanStatusEnum status;
    private Long createdById;
    private String createdByName;
    private Date createdAt;
    private Date startedAt;
    private Date completedAt;
    private String errorMessage;
    private Integer tableCount;
    private Integer relationCount;
}
