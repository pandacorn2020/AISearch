package com.aisearch;

import com.aisearch.config.KgProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(KgProperties.class)
public class AISearchApplication {
    public static void main(String[] args) {
        SpringApplication.run(AISearchApplication.class, args);
    }
}