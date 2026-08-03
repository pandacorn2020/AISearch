package com.aisearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kg")
public class KgProperties {
    private String modelName;
    private String apiKey;

    private String url;

    private String inputDir;

    private String ocrUrl;

    private String ocrModelName;

    private String ocrApiKey;

    private String ocrPrompt;

    /** OCR 模式：bigModel（大模型 OCR，默认） / aliDoc（阿里云文档智能） */
    private String ocrMode;

    /** 阿里云文档智能 AccessKey */
    private String aliDocAccessKey;

    /** 阿里云文档智能 SecretKey */
    private String aliDocSecretKey;

    /** 阿里云文档智能 Endpoint，默认 docmind-api.cn-hangzhou.aliyuncs.com */
    private String aliDocEndpoint;

    private Integer taskTimeoutSeconds;

    private Integer taskHeartbeatIntervalMs;
    public String getModelName() {
        return modelName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getUrl() {
        return url;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInputDir() {
        return inputDir;
    }

    public void setInputDir(String inputDir) {
        this.inputDir = inputDir;
    }

    public String getOcrUrl() {
        return ocrUrl;
    }

    public void setOcrUrl(String ocrUrl) {
        this.ocrUrl = ocrUrl;
    }

    public String getOcrModelName() {
        return ocrModelName;
    }

    public void setOcrModelName(String ocrModelName) {
        this.ocrModelName = ocrModelName;
    }

    public String getOcrApiKey() {
        return ocrApiKey;
    }

    public void setOcrApiKey(String ocrApiKey) {
        this.ocrApiKey = ocrApiKey;
    }

    public String getOcrPrompt() {
        return ocrPrompt;
    }

    public void setOcrPrompt(String ocrPrompt) {
        this.ocrPrompt = ocrPrompt;
    }

    public String getOcrMode() {
        if (ocrMode == null || ocrMode.trim().isEmpty()) {
            return "bigModel";
        }
        return ocrMode.trim();
    }

    public void setOcrMode(String ocrMode) {
        this.ocrMode = ocrMode;
    }

    public String getAliDocAccessKey() {
        return aliDocAccessKey;
    }

    public void setAliDocAccessKey(String aliDocAccessKey) {
        this.aliDocAccessKey = aliDocAccessKey;
    }

    public String getAliDocSecretKey() {
        return aliDocSecretKey;
    }

    public void setAliDocSecretKey(String aliDocSecretKey) {
        this.aliDocSecretKey = aliDocSecretKey;
    }

    public String getAliDocEndpoint() {
        if (aliDocEndpoint == null || aliDocEndpoint.trim().isEmpty()) {
            return "docmind-api.cn-hangzhou.aliyuncs.com";
        }
        return aliDocEndpoint.trim();
    }

    public void setAliDocEndpoint(String aliDocEndpoint) {
        this.aliDocEndpoint = aliDocEndpoint;
    }

    public Integer getTaskTimeoutSeconds() {
        if (taskTimeoutSeconds == null || taskTimeoutSeconds <= 0) {
            return 300;
        }
        return taskTimeoutSeconds;
    }

    public void setTaskTimeoutSeconds(Integer taskTimeoutSeconds) {
        this.taskTimeoutSeconds = taskTimeoutSeconds;
    }

    public Integer getTaskHeartbeatIntervalMs() {
        if (taskHeartbeatIntervalMs == null || taskHeartbeatIntervalMs <= 0) {
            return 5000;
        }
        return taskHeartbeatIntervalMs;
    }

    public void setTaskHeartbeatIntervalMs(Integer taskHeartbeatIntervalMs) {
        this.taskHeartbeatIntervalMs = taskHeartbeatIntervalMs;
    }

}