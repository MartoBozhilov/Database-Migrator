package com.database_migrator.config.database;

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
