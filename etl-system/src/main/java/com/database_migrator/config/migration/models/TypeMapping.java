package com.database_migrator.config.migration.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TypeMapping {
    private String targetType;
    private boolean dataLossRisk;
    private String warning;
}
