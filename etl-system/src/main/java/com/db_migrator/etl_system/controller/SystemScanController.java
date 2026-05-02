package com.db_migrator.etl_system.controller;

import com.db_migrator.etl_system.dto.request.SystemScanCreateRequest;
import com.db_migrator.etl_system.dto.response.SystemScanDetailsResponse;
import com.db_migrator.etl_system.dto.response.SystemScanResponse;
import com.db_migrator.etl_system.service.SystemScanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scans")
@RequiredArgsConstructor
public class SystemScanController {

    private final SystemScanService scanService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CONNECTOR_USER')")
    public ResponseEntity<SystemScanResponse> createScan(
            @Valid @RequestBody SystemScanCreateRequest request) {
        SystemScanResponse response = scanService.create(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CONNECTOR_USER')")
    public ResponseEntity<SystemScanResponse> startScan(@PathVariable Long id) {
        SystemScanResponse response = scanService.startScan(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CONNECTOR_USER')")
    public ResponseEntity<List<SystemScanResponse>> getAllScans() {
        List<SystemScanResponse> scans = scanService.findAll();
        return ResponseEntity.ok(scans);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CONNECTOR_USER')")
    public ResponseEntity<SystemScanResponse> getScan(@PathVariable Long id) {
        SystemScanResponse response = scanService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/details")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CONNECTOR_USER')")
    public ResponseEntity<SystemScanDetailsResponse> getScanDetails(@PathVariable Long id) {
        SystemScanDetailsResponse response = scanService.getDetails(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/connector/{connectorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CONNECTOR_USER')")
    public ResponseEntity<List<SystemScanResponse>> getScansByConnector(@PathVariable Long connectorId) {
        List<SystemScanResponse> scans = scanService.findByConnectorId(connectorId);
        return ResponseEntity.ok(scans);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CONNECTOR_USER')")
    public ResponseEntity<Void> deleteScan(@PathVariable Long id) {
        scanService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
