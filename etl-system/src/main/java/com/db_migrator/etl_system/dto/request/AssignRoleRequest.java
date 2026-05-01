package com.db_migrator.etl_system.dto.request;

import com.db_migrator.etl_system.model.enums.UserRoleEnum;
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
