package com.aisearch.dto;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class RequestContentKnowledgeSearchV4 {
    @JSONField(name = "query")
    private String query;

    @JSONField(name = "schema")
    private String schema;

    @JSONField(name = "categories")
    private List<String> categories;

    @JSONField(name = "max_community_count")
    private Integer maxCommunityCount;

    @JSONField(name = "max_entity_count")
    private Integer maxEntityCount;

    @JSONField(name = "max_segment_count")
    private Integer maxSegmentCount;

    @JSONField(name = "max_relationship_count")
    private Integer maxRelationshipCount;

    public RequestContentKnowledgeSearchV4() {
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public Integer getMaxCommunityCount() {
        return maxCommunityCount;
    }

    public void setMaxCommunityCount(Integer maxCommunityCount) {
        this.maxCommunityCount = maxCommunityCount;
    }

    public Integer getMaxEntityCount() {
        return maxEntityCount;
    }

    public void setMaxEntityCount(Integer maxEntityCount) {
        this.maxEntityCount = maxEntityCount;
    }

    public Integer getMaxSegmentCount() {
        return maxSegmentCount;
    }

    public void setMaxSegmentCount(Integer maxSegmentCount) {
        this.maxSegmentCount = maxSegmentCount;
    }

    public Integer getMaxRelationshipCount() {
        return maxRelationshipCount;
    }

    public void setMaxRelationshipCount(Integer maxRelationshipCount) {
        this.maxRelationshipCount = maxRelationshipCount;
    }

    @Override
    public String toString() {
        return "RequestContentKnowledgeSearchV4{" +
            "query='" + query + '\'' +
            ", schema='" + schema + '\'' +
            ", categories=" + categories +
            ", maxCommunityCount=" + maxCommunityCount +
            ", maxEntityCount=" + maxEntityCount +
            ", maxSegmentCount=" + maxSegmentCount +
            ", maxRelationshipCount=" + maxRelationshipCount +
            '}';
    }
}