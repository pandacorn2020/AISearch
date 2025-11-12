package com.aisearch.dto;

import com.alibaba.fastjson.annotation.JSONField;

public class RequestCommon<T> {
    @JSONField(name = "from_id")
    private String fromId;          // 用户id
    @JSONField(name = "from_nickname")
    private String fromNickname;
    // 用户昵称
    @JSONField(name = "content")
    private T content;             // 请求内容

    public RequestCommon() {}

    public String getFromId() {
        return fromId;
    }

    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    public String getFromNickname() {
        return fromNickname;
    }

    public void setFromNickname(String fromNickname) {
        this.fromNickname = fromNickname;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "RequestCommon{" +
            "fromId='" + fromId + '\'' +
            ", fromNickname='" + fromNickname + '\'' +
            ", content=" + content +
            '}';
    }
}
