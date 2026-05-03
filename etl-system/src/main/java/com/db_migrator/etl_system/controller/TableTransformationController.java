package com.db_migrator.etl_system.controller;

import com.db_migrator.etl_system.dto.request.TableTransformationRequest;
import com.db_migrator.etl_system.dto.response.TransformationModelDetailsResponse;
import com.db_migrator.etl_system.service.transformation.TransformationTableService;
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

    /**
     * ADD_TABLE - Add a new user-created table (not from scan)
     * POST /api/transformation-models/{modelId}/tables/add
     * Body: { "tableName": "my_new_table", "idGenerationStrategy": "AUTO_INCREMENT", "idColumnName": "audit_id" }
     */
    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<TransformationModelDetailsResponse> addTable(
            @PathVariable Long modelId,
            @Valid @RequestBody TableTransformationRequest request) {
        TransformationModelDetailsResponse response = tableService.addTable(modelId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * RENAME_TABLE - Rename a table
     * POST /api/transformation-models/{modelId}/tables/{tableId}/rename
     * Body: { "newName": "new_table_name" }
     */
    @PostMapping("/{tableId}/rename")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<TransformationModelDetailsResponse> renameTable(
            @PathVariable Long modelId,
            @PathVariable Long tableId,
            @Valid @RequestBody TableTransformationRequest request) {
        TransformationModelDetailsResponse response = tableService.renameTable(modelId, tableId, request.getNewName());
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE_TABLE - Mark table as deleted (soft delete)
     * POST /api/transformation-models/{modelId}/tables/{tableId}/delete
     */
    @PostMapping("/{tableId}/delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<TransformationModelDetailsResponse> deleteTable(
            @PathVariable Long modelId,
            @PathVariable Long tableId) {
        TransformationModelDetailsResponse response = tableService.deleteTable(modelId, tableId);
        return ResponseEntity.ok(response);
    }
}
