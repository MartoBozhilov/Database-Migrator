package com.database_migrator.config.migration.models;

import com.database_migrator.domain.connector.model.DatabaseTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MetadataQueryConfig {
    private DatabaseTypeEnum databaseType;
    private String tablesAndColumnsQuery;
    private String relationshipsQuery;
    private String tableCountQuery;
}
