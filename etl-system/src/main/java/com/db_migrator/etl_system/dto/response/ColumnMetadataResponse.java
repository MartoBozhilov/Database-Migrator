package com.db_migrator.etl_system.dto.response;

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
}
