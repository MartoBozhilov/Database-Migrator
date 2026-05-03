package com.db_migrator.etl_system.controller;

import com.db_migrator.etl_system.dto.request.TransformationModelCreateRequest;
import com.db_migrator.etl_system.dto.request.TransformationModelUpdateRequest;
import com.db_migrator.etl_system.dto.response.TransformationModelDetailsResponse;
import com.db_migrator.etl_system.dto.response.TransformationModelResponse;
import com.db_migrator.etl_system.service.TransformationModelService;
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
@RequestMapping("/api/transformation-models")
@RequiredArgsConstructor
public class TransformationModelController {

    private final TransformationModelService modelService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<TransformationModelDetailsResponse> createModel(
            @Valid @RequestBody TransformationModelCreateRequest request) {
        TransformationModelDetailsResponse response = modelService.create(request);
        return ResponseEntity.ok(response);
    }

    // Get all transformation models for current organization
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<List<TransformationModelResponse>> getAllModels() {
        List<TransformationModelResponse> models = modelService.findAll();
        return ResponseEntity.ok(models);
    }

    // Get transformation model by ID (basic info)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<TransformationModelResponse> getModel(@PathVariable Long id) {
        TransformationModelResponse response = modelService.findById(id);
        return ResponseEntity.ok(response);
    }

    // Get transformation model details (with all tables, columns, relations)
    @GetMapping("/{id}/details")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<TransformationModelDetailsResponse> getModelDetails(@PathVariable Long id) {
        TransformationModelDetailsResponse response = modelService.getDetails(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<TransformationModelDetailsResponse> updateModel(
            @PathVariable Long id,
            @Valid @RequestBody TransformationModelUpdateRequest request) {
        TransformationModelDetailsResponse response = modelService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<Void> deleteModel(@PathVariable Long id) {
        modelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
