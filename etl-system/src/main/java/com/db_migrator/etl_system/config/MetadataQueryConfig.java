package com.db_migrator.etl_system.config;

import com.db_migrator.etl_system.model.enums.DatabaseTypeEnum;
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
}
