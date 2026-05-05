package com.db_migrator.etl_system.controller;

import com.db_migrator.etl_system.dto.helper.ValidationResult;
import com.db_migrator.etl_system.dto.request.CycleCreateRequest;
import com.db_migrator.etl_system.dto.response.CycleDetailsResponse;
import com.db_migrator.etl_system.dto.response.CycleResponse;
import com.db_migrator.etl_system.service.CycleService;
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
@RequestMapping("/api/cycles")
@RequiredArgsConstructor
public class CycleController {

    private final CycleService cycleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CYCLE_EXECUTION_USER')")
    public ResponseEntity<CycleResponse> createCycle(@Valid @RequestBody CycleCreateRequest request) {
        CycleResponse response = cycleService.createCycle(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CYCLE_EXECUTION_USER')")
    public ResponseEntity<List<CycleResponse>> getAllCycles() {
        List<CycleResponse> cycles = cycleService.findAll();
        return ResponseEntity.ok(cycles);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CYCLE_EXECUTION_USER')")
    public ResponseEntity<CycleResponse> getCycle(@PathVariable Long id) {
        CycleResponse response = cycleService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/details")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CYCLE_EXECUTION_USER')")
    public ResponseEntity<CycleDetailsResponse> getCycleDetails(@PathVariable Long id) {
        CycleDetailsResponse response = cycleService.getDetails(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CYCLE_EXECUTION_USER')")
    public ResponseEntity<ValidationResult> validateCycle(@PathVariable Long id) {
        ValidationResult result = cycleService.validateCycle(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Execute cycle (start migration asynchronously)
     * Returns 202 Accepted immediately with cycle status RUNNING
     * User can poll GET /cycles/{id} to monitor progress
     */
    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CYCLE_EXECUTION_USER')")
    public ResponseEntity<CycleResponse> executeCycle(@PathVariable Long id) {
        cycleService.executeCycle(id);

        // Return updated cycle with status RUNNING
        CycleResponse response = cycleService.findById(id);
        return ResponseEntity.accepted().body(response);  // 202 Accepted
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'CYCLE_EXECUTION_USER')")
    public ResponseEntity<Void> deleteCycle(@PathVariable Long id) {
        cycleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
