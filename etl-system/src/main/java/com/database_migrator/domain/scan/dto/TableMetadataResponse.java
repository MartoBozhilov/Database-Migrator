package com.database_migrator.domain.scan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TableMetadataResponse {

    private Long id;
    private String tableName;
    private List<ColumnMetadataResponse> columns;
}
