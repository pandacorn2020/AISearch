package com.aisearch.controller;

import com.aisearch.entity.KGSegment;
import com.aisearch.exception.ResourceNotFoundException;
import com.aisearch.service.KGSegmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/segment")
public class KGSegmentController {
    @Autowired
    private KGSegmentService service;

    @GetMapping
    public Page<KGSegment> getAllSegments(
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
    public KGSegment getSegmentById(@PathVariable String schema,@PathVariable Long id) {
        return service.findById(schema,id).orElseThrow(() ->
            new ResourceNotFoundException("Segment with ID " + id + " not found"));
    }

    @PostMapping("/{schema}")
    public KGSegment createSegment(@PathVariable String schema,@RequestBody KGSegment segment) {
        return service.save(schema,segment);
    }

    @PutMapping("/{schema}/{id}")
    public KGSegment updateSegment(@PathVariable String schema,@PathVariable Long id, @RequestBody KGSegment segment) {
        if (!service.findById(schema,id).isPresent()) {
            throw new ResourceNotFoundException("Segment with ID " + id + " not found");
        }
        segment.setId(id);
        return service.save(schema,segment);
    }

    @DeleteMapping("/{schema}/{id}")
    public void deleteSegment(@PathVariable String schema,@PathVariable Long id) {
        if (!service.findById(schema,id).isPresent()) {
            throw new ResourceNotFoundException("Segment with ID " + id + " not found");
        }
        service.deleteById(schema,id);
    }
}