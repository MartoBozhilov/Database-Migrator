package com.db_migrator.etl_system.controller.admin;

import com.db_migrator.etl_system.dto.request.CreateOrganizationUserRequest;
import com.db_migrator.etl_system.dto.request.OrganizationCreateRequest;
import com.db_migrator.etl_system.dto.response.OrganizationResponse;
import com.db_migrator.etl_system.dto.response.UserResponse;
import com.db_migrator.etl_system.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(@Valid @RequestBody OrganizationCreateRequest request) {
        OrganizationResponse response = organizationService.create(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createMigrationAdmin(@Valid @RequestBody CreateOrganizationUserRequest request) {
        UserResponse response = organizationService.createMigrationAdmin(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> getAllOrganizations() {
        List<OrganizationResponse> organizations = organizationService.findAll();
        return ResponseEntity.ok(organizations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable Long id) {
        OrganizationResponse response = organizationService.findById(id);
        return ResponseEntity.ok(response);
    }
}
