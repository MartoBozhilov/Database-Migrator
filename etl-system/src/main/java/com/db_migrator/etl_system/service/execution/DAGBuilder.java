package com.db_migrator.etl_system.service.execution;

import com.db_migrator.etl_system.model.entity.transformation.TransformationModel;
import com.db_migrator.etl_system.model.entity.transformation.TransformationRelation;
import com.db_migrator.etl_system.model.entity.transformation.TransformationTable;
import com.db_migrator.etl_system.service.transformation.TransformationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DAG (Directed Acyclic Graph) Builder
 *
 * Builds dependency graph from transformation model foreign key relations
 * Used for:
 * 1. Cycle detection (pre-execution validation)
 * 2. Task scheduling (execution order)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DAGBuilder {

    /**
     * Build task dependency graph from transformation model
     *
     * @param model Transformation model with tables and relations
     * @return Map<tableName, Set<dependsOnTableNames>>
     *         e.g., {"orders" -> ["users", "products"], "users" -> []}
     */
    public Map<String, Set<String>> buildDependencyGraph(TransformationModel model) {
        Map<String, Set<String>> graph = new HashMap<>();

        // Get all non-excluded tables
        Set<String> includedTables = getIncludedTables(model);

        // Initialize graph nodes (all tables start with empty dependencies)
        for (String table : includedTables) {
            graph.put(table, new HashSet<>());
        }

        // Add edges from relations (foreign keys)
        for (TransformationRelation relation : model.getTransformationRelations()) {
            if (relation.getIsDeleted()) {
                continue; // Skip deleted relations
            }

            String foreignTable = relation.getForeignTable();
            String primaryTable = relation.getPrimaryTable();

            // Foreign table depends on primary table
            // (foreign table must be created AFTER primary table)
            if (includedTables.contains(foreignTable) && includedTables.contains(primaryTable)) {
                graph.get(foreignTable).add(primaryTable);
            }
        }

        log.debug("Built dependency graph with {} tables and {} dependencies",
                graph.size(), graph.values().stream().mapToInt(Set::size).sum());

        return graph;
    }

    /**
     * Validate graph is acyclic using DFS
     *
     * @param graph Dependency graph
     * @return true if graph has NO cycles (is acyclic), false if cycles exist
     */
    public boolean hasNoCycles(Map<String, Set<String>> graph) {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String node : graph.keySet()) {
            if (!visited.contains(node)) {
                if (hasCycleDFS(node, graph, visited, recursionStack)) {
                    log.warn("Cycle detected in dependency graph starting from table: {}", node);
                    return false; // Cycle found
                }
            }
        }

        return true; // No cycles (acyclic graph)
    }

    /**
     * Detect cycles and return cycle paths for detailed error messages
     *
     * @param graph Dependency graph
     * @return List of cycle paths (empty if no cycles)
     */
    public List<String> findCyclePaths(Map<String, Set<String>> graph) {
        List<String> cyclePaths = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        List<String> currentPath = new ArrayList<>();

        for (String node : graph.keySet()) {
            if (!visited.contains(node)) {
                findCyclesWithPath(node, graph, visited, recursionStack, currentPath, cyclePaths);
            }
        }

        return cyclePaths;
    }

    /**
     * DFS cycle detection with path tracking for detailed error messages
     */
    private boolean findCyclesWithPath(String node, Map<String, Set<String>> graph,
                                       Set<String> visited, Set<String> recursionStack,
                                       List<String> currentPath, List<String> cyclePaths) {
        visited.add(node);
        recursionStack.add(node);
        currentPath.add(node);

        Set<String> neighbors = graph.getOrDefault(node, new HashSet<>());
        for (String neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                if (findCyclesWithPath(neighbor, graph, visited, recursionStack, currentPath, cyclePaths)) {
                    return true;
                }
            } else if (recursionStack.contains(neighbor)) {
                // Cycle detected - build path string
                currentPath.add(neighbor);
                cyclePaths.add(String.join(" -> ", currentPath));
                currentPath.remove(currentPath.size() - 1);
                return true;
            }
        }

        recursionStack.remove(node);
        currentPath.remove(currentPath.size() - 1);
        return false;
    }

    /**
     * DFS cycle detection with recursion stack
     *
     * @param node Current node
     * @param graph Dependency graph
     * @param visited Visited nodes (across all DFS paths)
     * @param recursionStack Current path (recursion stack)
     * @return true if cycle detected, false otherwise
     */
    private boolean hasCycleDFS(String node, Map<String, Set<String>> graph,
                                Set<String> visited, Set<String> recursionStack) {
        visited.add(node);
        recursionStack.add(node);

        // Visit all dependencies
        for (String dependency : graph.getOrDefault(node, new HashSet<>())) {
            if (!visited.contains(dependency)) {
                // Recursive DFS
                if (hasCycleDFS(dependency, graph, visited, recursionStack)) {
                    return true; // Cycle found in recursive call
                }
            } else if (recursionStack.contains(dependency)) {
                // Back edge found - cycle detected!
                log.warn("Cycle detected: {} -> {}", node, dependency);
                return true;
            }
        }

        // Remove from recursion stack when backtracking
        recursionStack.remove(node);
        return false;
    }

    /**
     * Get list of included (non-excluded) table names from transformation model
     *
     * @param model Transformation model
     * @return Set of table names to be migrated
     */
    private Set<String> getIncludedTables(TransformationModel model) {
        Set<String> tables = new HashSet<>();

        for (TransformationTable table : model.getTransformationTables()) {
            if (!TransformationUtils.isTableDeleted(table)) {
                String tableName = TransformationUtils.getEffectiveTableName(table);
                if (tableName != null) {
                    tables.add(tableName);
                }
            }
        }

        return tables;
    }
}
