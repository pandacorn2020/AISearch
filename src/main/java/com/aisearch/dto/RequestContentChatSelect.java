package com.aisearch.dto;

import com.alibaba.fastjson.annotation.JSONField;

public class RequestContentChatSelect {

    @JSONField(name = "schema")
    private String schema;

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }
    @JSONField(name = "chat_type")
    private String chatType;

    @JSONField(name = "is_streaming")
    private Boolean isStreaming;
    public String getChatType() {
        return chatType;
    }
    public void setChatType(String chatType) {
        this.chatType = chatType;
    }
    @JSONField(name = "schema_description")
    private String schemaDescription;
    public String getSchemaDescription() {
        return schemaDescription;
    }

    public void setSchemaDescription(String schemaDescription) {
        this.schemaDescription = schemaDescription;
    }

    @Override
    public String toString() {
        return "RequestContentChatSelect{" +
            "schema='" + schema + '\'' +
            ", chatType='" + chatType + '\'' +
            ", isStreaming=" + isStreaming +
            '}';
    }
}
