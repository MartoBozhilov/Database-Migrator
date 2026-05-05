package com.db_migrator.etl_system.controller;

import com.db_migrator.etl_system.dto.request.ColumnAddRequest;
import com.db_migrator.etl_system.dto.request.ColumnChangeTypeRequest;
import com.db_migrator.etl_system.dto.request.ColumnRenameRequest;
import com.db_migrator.etl_system.dto.response.TransformationModelDetailsResponse;
import com.db_migrator.etl_system.service.transformation.TransformationColumnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transformation-models/{modelId}/tables/{tableId}/columns")
@RequiredArgsConstructor
public class ColumnTransformationController {

    private final TransformationColumnService columnService;

    @PutMapping("/{columnId}/rename")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<TransformationModelDetailsResponse> renameColumn(
            @PathVariable Long modelId,
            @PathVariable Long tableId,
            @PathVariable Long columnId,
            @Valid @RequestBody ColumnRenameRequest request) {
        TransformationModelDetailsResponse response = columnService.renameColumn(modelId, tableId, columnId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{columnId}/change-type")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<TransformationModelDetailsResponse> changeColumnType(
            @PathVariable Long modelId,
            @PathVariable Long tableId,
            @PathVariable Long columnId,
            @Valid @RequestBody ColumnChangeTypeRequest request) {
        TransformationModelDetailsResponse response = columnService.changeColumnType(modelId, tableId, columnId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<TransformationModelDetailsResponse> addColumn(
            @PathVariable Long modelId,
            @PathVariable Long tableId,
            @Valid @RequestBody ColumnAddRequest request) {
        TransformationModelDetailsResponse response = columnService.addColumn(modelId, tableId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{columnId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<TransformationModelDetailsResponse> deleteColumn(
            @PathVariable Long modelId,
            @PathVariable Long tableId,
            @PathVariable Long columnId) {
        TransformationModelDetailsResponse response = columnService.deleteColumn(modelId, tableId, columnId);
        return ResponseEntity.ok(response);
    }
}
