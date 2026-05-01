package com.db_migrator.etl_system.controller.admin;

import com.db_migrator.etl_system.dto.request.AssignRoleRequest;
import com.db_migrator.etl_system.dto.response.UserResponse;
import com.db_migrator.etl_system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @PostMapping("/{id}/roles")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable Long id,
            @Valid @RequestBody AssignRoleRequest request) {
        UserResponse response = userService.assignRole(id, request.getRole());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/roles")
    public ResponseEntity<UserResponse> removeRole(
            @PathVariable Long id,
            @Valid @RequestBody AssignRoleRequest request) {
        UserResponse response = userService.removeRole(id, request.getRole());
        return ResponseEntity.ok(response);
    }
}
