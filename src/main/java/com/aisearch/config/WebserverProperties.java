package com.aisearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "webserver")
public class WebserverProperties {

    private String imageUrlPrefix;

    private String llmEngine;
    public String getImageUrlPrefix() {
        return imageUrlPrefix;
    }
    public String getLlmEngine() {
        return llmEngine;
    }


    public void setImageUrlPrefix(String imageUrlPrefix) {
        this.imageUrlPrefix = imageUrlPrefix;
    }

    public void setLlmEngine(String llmEngine) {
        this.llmEngine = llmEngine;
    }

}