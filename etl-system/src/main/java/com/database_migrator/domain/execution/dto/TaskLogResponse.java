package com.database_migrator.domain.execution.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskLogResponse {

    private Long id;
    private Date timestamp;
    private String level;
    private String message;
}
