package com.aisearch.controller;

import com.aisearch.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SchemaController {

    @Autowired
    private SchemaService schemaService;

    @PostMapping("/initialize-schemas")
    public String initializeSchemas() {
        schemaService.initializeSchemas();
        return "Schemas initialized successfully";
    }
}