package com.db_migrator.etl_system.config;

import com.db_migrator.etl_system.model.enums.DatabaseTypeEnum;
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
