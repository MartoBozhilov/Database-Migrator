package com.database_migrator.domain.execution.dto;

import com.database_migrator.domain.execution.model.CycleStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CycleResponse {

    private Long id;
    private String name;
    private Long transformationModelId;
    private String transformationModelName;
    private Long targetConnectorId;
    private String targetConnectorName;
    private CycleStatusEnum status;
    private Date createdAt;
    private Date startedAt;
    private Date completedAt;
    private String errorMessage;
    private Integer totalTasks;
    private Integer taskCount;
    private Integer completedTasks;
    private Integer failedTasks;
    private Long createdById;
    private String createdByName;
}
