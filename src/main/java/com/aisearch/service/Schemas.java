package com.aisearch.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Schemas {
    public static final String DOCS = "DOCS";

    public static final String[] SCHEMAS = {DOCS};
    public static final Map<String, String> SCHEMA_DIR_MAP = new HashMap<>();

    public static final Map<String, String> SCHEMA_DESCRIPTION_MAP = new HashMap<>();

    static {
        SCHEMA_DIR_MAP.put(DOCS, "docs");

        SCHEMA_DESCRIPTION_MAP.put(DOCS, "文档");
    }

    public static String getSchemaDir(String schema) {
        return SCHEMA_DIR_MAP.get(schema);
    }

    public static String getSchemaDescription(String schema) {
        return SCHEMA_DESCRIPTION_MAP.get(schema);
    }

    public static Collection<String> allSchemas() {
        return Collections.unmodifiableCollection(SCHEMA_DIR_MAP.keySet());
    }


}
