package com.aisearch.controller;

import com.aisearch.entity.KGCommunity;
import com.aisearch.exception.ResourceNotFoundException;
import com.aisearch.service.KGCommunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community")
public class KGCommunityController {
    @Autowired
    private KGCommunityService service;

    @GetMapping
    public Page<KGCommunity> getAll(
        @RequestParam String schema,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "asc") String direction) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return service.findAll(schema,pageable);
    }

    @GetMapping("/{schema}/{id}")
    public KGCommunity getCommunityById(@PathVariable String schema,@PathVariable Long id) {
        return service.findById(schema,id).orElseThrow(() ->
            new ResourceNotFoundException("Community with ID " + id + " not found"));
    }

    @PostMapping("/{schema}")
    public KGCommunity createCommunity(@PathVariable String schema,@RequestBody KGCommunity community) {
        return service.save(schema,community);
    }

    @PutMapping("/{schema}/{id}")
    public KGCommunity updateCommunity(@PathVariable String schema,@PathVariable Long id, @RequestBody KGCommunity community) {
        if (!service.findById(schema,id).isPresent()) {
            throw new ResourceNotFoundException("Community with ID " + id + " not found");
        }
        community.setId(id);
        return service.save(schema,community);
    }

    @DeleteMapping("/{schema}/{id}")
    public void deleteCommunity(@PathVariable String schema,@PathVariable Long id) {
        if (!service.findById(schema,id).isPresent()) {
            throw new ResourceNotFoundException("Community with ID " + id + " not found");
        }
        service.deleteById(schema,id);
    }
}