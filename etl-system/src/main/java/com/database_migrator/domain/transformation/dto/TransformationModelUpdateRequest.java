package com.database_migrator.domain.transformation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransformationModelUpdateRequest {
    @NotBlank(message = "Name is required")
    private String name;
}
