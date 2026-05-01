package com.db_migrator.etl_system.controller;

import com.db_migrator.etl_system.dto.response.OrganizationResponse;
import com.db_migrator.etl_system.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable Long id) {
        OrganizationResponse response = organizationService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<OrganizationResponse> getOrganizationByName(@PathVariable String name) {
        OrganizationResponse response = organizationService.findByName(name);
        return ResponseEntity.ok(response);
    }
}
