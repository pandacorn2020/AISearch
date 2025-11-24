package com.aisearch.llm;

import com.aisearch.config.KgProperties;
import com.aisearch.config.QueryProperties;
import com.aisearch.config.ReportProperties;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.logging.Logger;

@Component
public class LLMModel {

    private static final Logger logger = Logger.getLogger(LLMModel.class.getSimpleName());

    @Autowired
    private KgProperties kgProperties;

    @Autowired
    private QueryProperties queryProperties;
    @Autowired
    private ReportProperties reportProperties;

    public ChatModel buildKgModel() {
        logger.log(java.util.logging.Level.INFO, "LLMModel.buildModel: url=" + kgProperties.getUrl() +
                ", apiKey=" + kgProperties.getApiKey() +
                ", modelName=" + kgProperties.getModelName());
        return buildModel(kgProperties.getUrl(),
                kgProperties.getApiKey(),
                kgProperties.getModelName());
    }

    public StreamingChatModel buildStreamingModel() {
        logger.log(java.util.logging.Level.INFO, "LLMModel.buildStreamingModel: url=" + queryProperties.getUrl() +
                ", apiKey=" + queryProperties.getApiKey() +
                ", modelName=" + queryProperties.getModelName());
        return buildStreamingModel(queryProperties.getUrl(),
                queryProperties.getApiKey(),
                queryProperties.getModelName());
    }

    public ChatModel buildQueryChatModel() {
        logger.log(java.util.logging.Level.INFO, "LLMModel.buildModel: url=" + queryProperties.getUrl() +
                ", apiKey=" + queryProperties.getApiKey() +
                ", modelName=" + queryProperties.getModelName());
        return buildModel(queryProperties.getUrl(),
                queryProperties.getApiKey(),
                queryProperties.getModelName());
    }

    public ChatModel buildModel(String url, String apiKey, String modelName) {
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder();
        if (url != null) {
            builder = builder.baseUrl(url);
        }
        builder = builder.apiKey(apiKey)
                .modelName(modelName);
        builder.timeout(Duration.ofSeconds(3600));
        builder.maxRetries(5);
        return builder.build();
    }

    public StreamingChatModel buildStreamingModel(String url, String apiKey,
                                                          String modelName) {
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder =
                OpenAiStreamingChatModel.builder();
        if (url != null) {
            builder = builder.baseUrl(url);
        }
        builder = builder.apiKey(apiKey)
                .modelName(modelName);
        builder.timeout(Duration.ofSeconds(3600));
        builder.logRequests(true);
        builder.logResponses(true);
        return builder.build();
    }

}
