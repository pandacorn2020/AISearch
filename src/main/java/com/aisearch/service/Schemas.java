package com.aisearch.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Schemas {
    public static final String DOCS = "DOCS_JT"; // 测试schema
    public static final String MANAGER = "aisearch_manager"; // 系统schema，用于任务状态管理

    public static final String[] SCHEMAS = {DOCS};

    public static final Map<String, String> SCHEMA_DESCRIPTION_MAP = new HashMap<>();

    static {
        SCHEMA_DESCRIPTION_MAP.put(DOCS, "文档");
        SCHEMA_DESCRIPTION_MAP.put(MANAGER, "系统管理");
    }

    public static String getSchemaDescription(String schema) {
        return SCHEMA_DESCRIPTION_MAP.get(schema);
    }


}
