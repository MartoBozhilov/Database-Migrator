package com.db_migrator.etl_system.dto.response;

import com.db_migrator.etl_system.model.enums.CycleStatusEnum;
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
    private CycleStatusEnum status;
    private Date createdAt;
    private Date startedAt;
    private Date completedAt;
    private String errorMessage;
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer failedTasks;
    private Long createdById;
    private String createdByName;
}
