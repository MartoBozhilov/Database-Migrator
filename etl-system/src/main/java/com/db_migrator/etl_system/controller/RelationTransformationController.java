package com.db_migrator.etl_system.controller;

import com.db_migrator.etl_system.dto.request.RelationAddRequest;
import com.db_migrator.etl_system.dto.response.TransformationModelDetailsResponse;
import com.db_migrator.etl_system.service.transformation.TransformationRelationService;
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
@RequestMapping("/api/transformation-models/{modelId}/relations")
@RequiredArgsConstructor
public class RelationTransformationController {

    private final TransformationRelationService relationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<TransformationModelDetailsResponse> addRelation(
            @PathVariable Long modelId,
            @Valid @RequestBody RelationAddRequest request) {
        TransformationModelDetailsResponse response = relationService.addRelation(modelId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{relationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MIGRATION_ADMIN', 'TRANSFORMATION_MODEL_USER')")
    public ResponseEntity<TransformationModelDetailsResponse> deleteRelation(
            @PathVariable Long modelId,
            @PathVariable Long relationId) {
        TransformationModelDetailsResponse response = relationService.deleteRelation(modelId, relationId);
        return ResponseEntity.ok(response);
    }
}
