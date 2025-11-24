package com.aisearch.controller;

import com.aisearch.entity.KGRelationship;
import com.aisearch.entity.KGRelationshipKey;
import com.aisearch.exception.ResourceNotFoundException;
import com.aisearch.service.KGRelationshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/relationship")
public class KGRelationshipController {
    @Autowired
    private KGRelationshipService service;

    @GetMapping
    public Page<KGRelationship> getAllRelationships(
        @RequestParam String schema,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "fileName") String sortBy,
        @RequestParam(defaultValue = "asc") String direction) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return service.findAll(schema,pageable);
    }

    @GetMapping("/{schema}/{source}/{target}/{relation}")
    public KGRelationship getRelationshipById(@PathVariable String schema,@PathVariable String source, @PathVariable String target, @PathVariable String relation) {
        KGRelationshipKey id = new KGRelationshipKey(source, target, relation);
        return service.findById(schema,id).orElseThrow(() ->
            new ResourceNotFoundException("Relationship with ID " + id + " not found"));
    }

    @PostMapping("/{schema}")
    public KGRelationship createRelationship(@PathVariable String schema,@RequestBody KGRelationship relationship) {
        return service.save(schema,relationship);
    }

    @PutMapping("/{schema}/{source}/{target}/{relation}")
    public KGRelationship updateRelationship(@PathVariable String schema,@PathVariable String source, @PathVariable String target, @PathVariable String relation, @RequestBody KGRelationship relationship) {
        KGRelationshipKey id = new KGRelationshipKey(source, target, relation);
        if (!service.findById(schema,id).isPresent()) {
            throw new ResourceNotFoundException("Relationship with ID " + id + " not found");
        }
        relationship.setId(id);
        return service.save(schema,relationship);
    }

    @DeleteMapping("/{schema}/{source}/{target}/{relation}")
    public void deleteRelationship(@PathVariable String schema,@PathVariable String source, @PathVariable String target, @PathVariable String relation) {
        KGRelationshipKey id = new KGRelationshipKey(source, target, relation);
        if (!service.findById(schema,id).isPresent()) {
            throw new ResourceNotFoundException("Relationship with ID " + id + " not found");
        }
        service.deleteById(schema,id);
    }
}