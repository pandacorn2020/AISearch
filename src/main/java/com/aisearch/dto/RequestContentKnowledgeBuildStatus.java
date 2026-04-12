package com.aisearch.dto;

import com.alibaba.fastjson.annotation.JSONField;

public class RequestContentKnowledgeBuildStatus {
    @JSONField(name = "schema")
    private String schema;

    @JSONField(name = "file_name")
    private String fileName;

    public RequestContentKnowledgeBuildStatus() {}

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "RequestContentKnowledgeBuildStatus{" +
            "schema='" + schema + '\'' +
            ", fileName='" + fileName + '\'' +
            '}';
    }
}
