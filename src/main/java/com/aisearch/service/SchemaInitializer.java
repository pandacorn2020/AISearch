package com.aisearch.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SchemaInitializer {

    @Autowired
    private SchemaService schemaService;

    @PostConstruct
    public void initDefaultSchemas() {
        schemaService.initializeSchemas(Schemas.SCHEMAS);
        schemaService.initializeManagerSchema();
    }
}
