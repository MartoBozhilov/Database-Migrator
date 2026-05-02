package com.db_migrator.etl_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RelationMetadataResponse {

    private Long id;
    private String foreignTable;
    private String foreignColumn;
    private String primaryTable;
    private String primaryColumn;
}
