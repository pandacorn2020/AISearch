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

}