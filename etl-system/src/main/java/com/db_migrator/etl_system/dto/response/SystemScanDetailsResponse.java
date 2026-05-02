package com.db_migrator.etl_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemScanDetailsResponse extends SystemScanResponse {

    private List<TableMetadataResponse> tables;
    private List<RelationMetadataResponse> relations;
}
