package com.db_migrator.etl_system.controller;

import com.db_migrator.etl_system.dto.request.ConnectorCreateRequest;
import com.db_migrator.etl_system.dto.request.ConnectorUpdateRequest;
import com.db_migrator.etl_system.dto.response.ConnectorResponse;
import com.db_migrator.etl_system.dto.response.ConnectionTestResponse;
import com.db_migrator.etl_system.service.ConnectorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/connectors")
@RequiredArgsConstructor
public class ConnectorController {

    private final ConnectorService connectorService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CONNECTOR_USER')")
    public ResponseEntity<ConnectorResponse> createConnector(
            @Valid @RequestBody ConnectorCreateRequest request) {
        ConnectorResponse response = connectorService.create(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CONNECTOR_USER')")
    public ResponseEntity<List<ConnectorResponse>> getAllConnectors() {
        List<ConnectorResponse> connectors = connectorService.findAll();
        return ResponseEntity.ok(connectors);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CONNECTOR_USER')")
    public ResponseEntity<ConnectorResponse> getConnector(@PathVariable Long id) {
        ConnectorResponse response = connectorService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CONNECTOR_USER')")
    public ResponseEntity<ConnectorResponse> updateConnector(
            @PathVariable Long id,
            @Valid @RequestBody ConnectorUpdateRequest request) {
        ConnectorResponse response = connectorService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CONNECTOR_USER')")
    public ResponseEntity<Void> deleteConnector(@PathVariable Long id) {
        connectorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CONNECTOR_USER')")
    public ResponseEntity<ConnectionTestResponse> testConnection(@PathVariable Long id) {
        ConnectionTestResponse response = connectorService.testConnection(id);
        return ResponseEntity.ok(response);
    }
}
