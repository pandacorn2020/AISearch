package com.aisearch.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Schemas {
    public static final String DOCS = "DOCS_JT";

    public static final String[] SCHEMAS = {DOCS};

    public static final Map<String, String> SCHEMA_DESCRIPTION_MAP = new HashMap<>();

    static {
        SCHEMA_DESCRIPTION_MAP.put(DOCS, "文档");
    }

    public static String getSchemaDescription(String schema) {
        return SCHEMA_DESCRIPTION_MAP.get(schema);
    }


}
