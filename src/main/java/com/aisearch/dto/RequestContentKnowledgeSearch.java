package com.aisearch.dto;

import com.alibaba.fastjson.annotation.JSONField;

public class RequestContentKnowledgeSearch {
    @JSONField(name = "query")
    private String query;

    public RequestContentKnowledgeSearch() {}

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @JSONField(name = "schema")
    private String schema;

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @Override
    public String toString() {
        return "RequestContentKnowledgeSearch{" +
            "query='" + query + '\'' +
            ", schema='" + schema + '\'' +
            '}';
    }
}
