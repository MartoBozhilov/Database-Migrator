package com.database_migrator.domain.transformation.controller;

import com.database_migrator.domain.transformation.dto.TableTransformationRequest;
import com.database_migrator.domain.transformation.dto.TransformationModelDetailsResponse;
import com.database_migrator.domain.transformation.service.TransformationTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transformation-models/{modelId}/tables")
@RequiredArgsConstructor
public class TableTransformationController {

    private final TransformationTableService tableService;

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<TransformationModelDetailsResponse> addTable(
            @PathVariable Long modelId,
            @Valid @RequestBody TableTransformationRequest request) {
        TransformationModelDetailsResponse response = tableService.addTable(modelId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{tableId}/rename")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<TransformationModelDetailsResponse> renameTable(
            @PathVariable Long modelId,
            @PathVariable Long tableId,
            @Valid @RequestBody TableTransformationRequest request) {
        TransformationModelDetailsResponse response = tableService.renameTable(modelId, tableId, request.getNewName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{tableId}/delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<TransformationModelDetailsResponse> deleteTable(
            @PathVariable Long modelId,
            @PathVariable Long tableId) {
        TransformationModelDetailsResponse response = tableService.deleteTable(modelId, tableId);
        return ResponseEntity.ok(response);
    }
}
