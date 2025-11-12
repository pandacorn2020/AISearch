package com.aisearch.dto;

import com.alibaba.fastjson.annotation.JSONField;

public class RequestContentKnowledgeReset {
    @JSONField(name = "schema")
    private String schema;

    public RequestContentKnowledgeReset() {}

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @Override
    public String toString() {
        return "TestSchema{" +
            "schema='" + schema + '\'' +
            '}';
    }
}
