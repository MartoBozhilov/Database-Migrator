package com.db_migrator.etl_system.service.transformation.validation;

import com.db_migrator.etl_system.model.entity.transformation.TransformationModel;
import com.db_migrator.etl_system.model.entity.transformation.TransformationRelation;
import com.db_migrator.etl_system.model.entity.transformation.TransformationTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.db_migrator.etl_system.service.transformation.TransformationUtils.getEffectiveTableName;
import static com.db_migrator.etl_system.service.transformation.TransformationUtils.isTableDeleted;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransformationValidationService {

    /**
     * Validate transformation model before confirmation
     * Returns list of validation errors (empty if valid)
     */
    public List<String> validateForConfirmation(TransformationModel model) {

        // Check for DAG cycles
        List<String> cycleErrors = validateDAG(model);
        List<String> errors = new ArrayList<>(cycleErrors);

        // Add more validations here as needed for Phase 5

        return errors;
    }

    /**
     * Validate that relationship graph is acyclic (DAG requirement)
     * Returns list of cycle descriptions (empty if no cycles)
     */
    private List<String> validateDAG(TransformationModel model) {
        List<String> errors = new ArrayList<>();

        // Build graph from relations
        Map<String, Set<String>> graph = buildRelationshipGraph(model);

        // Detect cycles using DFS
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        List<String> cyclePath = new ArrayList<>();

        for (String table : graph.keySet()) {
            if (!visited.contains(table)) {
                if (hasCycleDFS(table, graph, visited, recursionStack, cyclePath)) {
                    errors.add("Cycle detected in relationships: " + String.join(" -> ", cyclePath));
                    cyclePath.clear();
                }
            }
        }

        return errors;
    }

    /**
     * Build directed graph from transformation relations
     * Key: foreign table, Value: set of primary tables it references
     */
    private Map<String, Set<String>> buildRelationshipGraph(TransformationModel model) {
        Map<String, Set<String>> graph = new HashMap<>();

        // Get all non-deleted, included tables
        Set<String> includedTables = getIncludedTables(model);

        // Add edges from active relations
        for (TransformationRelation relation : model.getTransformationRelations()) {
            if (relation.getIsDeleted()) {
                continue; // Skip deleted relations
            }

            String foreignTable = relation.getForeignTable();
            String primaryTable = relation.getPrimaryTable();

            // Only include if both tables are included in transformation
            if (includedTables.contains(foreignTable) && includedTables.contains(primaryTable)) {
                graph.computeIfAbsent(foreignTable, k -> new HashSet<>()).add(primaryTable);

                // Ensure primary table exists in graph even if it has no outgoing edges
                graph.putIfAbsent(primaryTable, new HashSet<>());
            }
        }

        return graph;
    }

    /**
     * Get set of table names that are included in transformation (not deleted)
     */
    private Set<String> getIncludedTables(TransformationModel model) {
        Set<String> includedTables = new HashSet<>();

        for (TransformationTable table : model.getTransformationTables()) {
            if (!isTableDeleted(table)) {
                String tableName = getEffectiveTableName(table);
                if (tableName != null) {
                    includedTables.add(tableName);
                }
            }
        }

        return includedTables;
    }

    /**
     * DFS cycle detection with path tracking
     */
    private boolean hasCycleDFS(String table, Map<String, Set<String>> graph,
                                Set<String> visited, Set<String> recursionStack,
                                List<String> cyclePath) {
        visited.add(table);
        recursionStack.add(table);
        cyclePath.add(table);

        Set<String> neighbors = graph.getOrDefault(table, new HashSet<>());
        for (String neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                if (hasCycleDFS(neighbor, graph, visited, recursionStack, cyclePath)) {
                    return true;
                }
            } else if (recursionStack.contains(neighbor)) {
                // Cycle detected - add neighbor to complete the cycle
                cyclePath.add(neighbor);
                return true;
            }
        }

        recursionStack.remove(table);
        cyclePath.removeLast();
        return false;
    }
}
