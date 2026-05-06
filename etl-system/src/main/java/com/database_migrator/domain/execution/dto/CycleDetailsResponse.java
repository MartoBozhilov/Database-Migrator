package com.database_migrator.domain.execution.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CycleDetailsResponse extends CycleResponse {

    private List<TaskResponse> tasks;
}
