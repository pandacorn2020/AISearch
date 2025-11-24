package com.aisearch.controller;

import com.aisearch.entity.KGEntity;
import com.aisearch.exception.ResourceNotFoundException;
import com.aisearch.service.KGEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entity")
public class KGEntityController {
    @Autowired
    private KGEntityService service;

    @GetMapping
    public Page<KGEntity> getAll(
        @RequestParam String schema,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "name") String sortBy,
        @RequestParam(defaultValue = "asc") String direction) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return service.findAll(schema,pageable);
    }

    @GetMapping("/{schema}/{name}")
    public KGEntity getById(@PathVariable String schema,@PathVariable String name) {
        return service.findById(schema,name).orElseThrow(() ->
            new ResourceNotFoundException("Entity with name " + name + " not found"));
    }

    @PostMapping("/{schema}")
    public KGEntity create(@PathVariable String schema,@RequestBody KGEntity entity) {
        return service.save(schema,entity);
    }

    @PutMapping("/{schema}/{name}")
    public KGEntity update(@PathVariable String schema,@PathVariable String name, @RequestBody KGEntity entity) {
        if (!service.findById(schema,name).isPresent()) {
            throw new ResourceNotFoundException("Entity with name " + name + " not found");
        }
        entity.setName(name);
        return service.save(schema,entity);
    }

    @DeleteMapping("/{schema}/{name}")
    public void delete(@PathVariable String schema,@PathVariable String name) {
        if (!service.findById(schema,name).isPresent()) {
            throw new ResourceNotFoundException("Entity with name " + name + " not found");
        }
        service.deleteById(schema,name);
    }
}