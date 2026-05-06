package com.database_migrator.domain.auth.dto;

import com.database_migrator.domain.auth.model.UserRoleEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignRoleRequest {

    @NotNull(message = "Role is required")
    private UserRoleEnum role;
}
