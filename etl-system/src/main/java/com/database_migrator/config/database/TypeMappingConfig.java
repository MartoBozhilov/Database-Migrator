package com.database_migrator.config.database;

import com.database_migrator.domain.connector.model.DatabaseTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TypeMappingConfig {
    private DatabaseTypeEnum sourceDatabase;
    private DatabaseTypeEnum targetDatabase;
    private Map<String, List<TypeMapping>> mappings;
}
