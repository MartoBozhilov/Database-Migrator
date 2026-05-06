package com.database_migrator.domain.scan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ColumnMetadataResponse {

    private Long id;
    private String columnName;
    private String dataType;
    private Boolean isNullable;
    private Integer length;
    private Boolean isPrimaryKey;
    private Boolean isAutoIncrement;
}
