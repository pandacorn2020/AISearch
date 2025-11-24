package com.aisearch.controller;

import com.aisearch.entity.KGEntity;
import com.aisearch.entity.KGRelationship;
import com.aisearch.service.KGEntityService;
import com.aisearch.service.KGRelationshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/graph")
public class KGGraphController {

    @Autowired
    private KGEntityService entityService;

    @Autowired
    private KGRelationshipService relationshipService;

    @GetMapping("/{schema}/{entityName}")
    public Map<String, Object> getRelatedEntities(
        @PathVariable String schema,
        @PathVariable String entityName,
        @RequestParam(defaultValue = "1") int depth) {

        // Validate depth
        if (depth < 1) {
            throw new IllegalArgumentException("Depth must be at least 1");
        }

        // Initialize result containers
        Set<String> visitedEntities = new HashSet<>();
        List<KGRelationship> allRelationships = new ArrayList<>();
        Map<String, KGEntity> allEntities = new HashMap<>();

        // Recursive function to fetch relationships and entities
        fetchRelatedEntities(schema,entityName, depth, visitedEntities, allRelationships, allEntities);

        // Prepare the response
        Map<String, Object> graphData = new HashMap<>();
        graphData.put("entities", allEntities.values());
        graphData.put("relationships", allRelationships);

        return graphData;
    }

    private void fetchRelatedEntities(
        String schema,
        String entityName,
        int depth,
        Set<String> visitedEntities,
        List<KGRelationship> allRelationships,
        Map<String, KGEntity> allEntities) {

        // Stop recursion if depth is 0 or entity already visited
        if (depth == 0 || visitedEntities.contains(entityName)) {
            return;
        }

        // Mark entity as visited
        visitedEntities.add(entityName);

        // Fetch the entity
        entityService.findById(schema,entityName).ifPresent(entity -> allEntities.put(entityName, entity));

        // Fetch relationships where the entity is either the source or target
        List<KGRelationship> relationships = relationshipService.findBySourceOrTarget(schema,entityName);
        allRelationships.addAll(relationships);

        // Recursively fetch related entities
        for (KGRelationship relationship : relationships) {
            fetchRelatedEntities(schema,relationship.getSource(), depth - 1, visitedEntities, allRelationships, allEntities);
            fetchRelatedEntities(schema,relationship.getTarget(), depth - 1, visitedEntities, allRelationships, allEntities);
        }
    }
}