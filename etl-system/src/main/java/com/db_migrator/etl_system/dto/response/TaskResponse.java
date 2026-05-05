package com.db_migrator.etl_system.dto.response;

import com.db_migrator.etl_system.model.enums.TaskStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private Long id;
    private String tableName;
    private TaskStatusEnum status;
    private Date startedAt;
    private Date completedAt;
    private String errorMessage;
    private Long rowsProcessed;
    private List<String> dependsOn;
}
