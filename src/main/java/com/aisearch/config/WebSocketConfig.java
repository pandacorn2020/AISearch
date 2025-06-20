package com.aisearch.config;

import com.aisearch.llm.CustomWebSocketHandler;
import com.aisearch.llm.LLMModel;
import com.aisearch.service.DocumentLoader;
import com.aisearch.service.GraphSearch;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private LLMModel llmModel;

    @Autowired
    private DocumentLoader documentLoader;

    @Autowired
    private GraphSearch graphSearch;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        StreamingChatLanguageModel model =
                llmModel.buildStreamingModel();
        String systemPrompt = documentLoader.readSystemPrompt();
        registry.addHandler(new CustomWebSocketHandler(model, systemPrompt,
                        graphSearch),
                "/chat").setAllowedOrigins("http://localhost:3000") // Allow requests from localhost:3000
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
