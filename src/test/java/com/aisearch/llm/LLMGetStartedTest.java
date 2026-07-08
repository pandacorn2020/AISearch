package com.aisearch.llm;

import java.util.HashMap;
import java.util.Map;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import org.junit.jupiter.api.Test;

public class LLMGetStartedTest {


    @Test
    public void test() {

        OpenAiChatRequestParameters params = OpenAiChatRequestParameters.builder()
            .customParameters(Map.of("enable_thinking", false))
            .build();
        ChatRequest chatRequest = ChatRequest.builder()
            .messages(UserMessage.userMessage("你好"))
            .parameters(params)
            .build();

        OpenAiChatModel model = OpenAiChatModel.builder()
            .baseUrl("https://llm-dne5kw6zs5l4slh0.cn-beijing.maas.aliyuncs.com/compatible-mode/v1")
            .apiKey("sk-3ce549b0b88846bca3d3e608d046706d")
            .modelName("qwen3-32b")
            .build();

        ChatResponse aiResponse = model.chat(chatRequest);
        System.out.println(aiResponse.aiMessage().text());
    }
    @Test
    public void testLiantai() {

        OpenAiChatRequestParameters params = OpenAiChatRequestParameters.builder()
            .customParameters(Map.of("chat_template_kwargs", Map.of(
                "enable_thinking", false
            )))
            .build();
        ChatRequest chatRequest = ChatRequest.builder()
            .messages(UserMessage.userMessage("你好"))
            .parameters(params)
            .build();

        OpenAiChatModel model = OpenAiChatModel.builder()
            .baseUrl("https://model-inference-8ef84e18.a.user.hmlt.ltaidc.com:8444/v1")
            .apiKey("sk-9cc9d")
            .modelName("Qwen3-32B-local")
            .build();

        ChatResponse aiResponse = model.chat(chatRequest);

        System.out.println(aiResponse.aiMessage().text());
    }

    public static void main(String[] args) {

        OpenAiChatModel model = OpenAiChatModel.builder()
            .baseUrl("http://langchain4j.dev/demo/openai/v1")
            .apiKey("demo")
            .modelName("gpt-4o-mini")
            .build();

        String answer = model.chat("你好");
        System.out.println(answer);


//        kg.modelName=qwen3-32b
//        kg.apiKey=sk-9cc9dbdd16b3488e9edb1cbad7ea695a
//        kg.url=https://dashscope.aliyuncs.com/compatible-mode/v1
    }
}
