package com.aisearch.llm;

import com.aisearch.config.WebserverProperties;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.output.Response;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Callable;

public class KgTask implements Callable<String> {
    private ChatMessage systemMessage;
    private String ragKgSystemPrompt;

    private String ragKgUserPrompt;

    private TextSegment segment;

    private String text;

    private ChatModel model;



    private static final Logger logger = LoggerFactory.getLogger(KgTask.class.getSimpleName());



    public KgTask(ChatMessage systemMessage,
                  String ragKgSystemPrompt,
                  String ragKgUserPrompt,
                  TextSegment segment,
                  ChatModel model) {
        this.systemMessage = systemMessage;
        this.segment = segment;
        this.ragKgSystemPrompt = ragKgSystemPrompt;
        this.ragKgUserPrompt = ragKgUserPrompt;
        this.model = model;
        if (segment.text().length() >= 8000) {
            System.out.println("break;");
        }
    }

    public String getText() {
        return text;
    }

    @Override
    public String call() {
        try {

            ChatMessage systemMessage = SystemMessage.systemMessage(ragKgSystemPrompt);
            String message = String.format(ragKgUserPrompt, segment.text());
            ChatMessage userMessage = UserMessage.userMessage(message);
            // 日志存储请求发起的时间和结束的时间，并算耗时
            long startTime = System.currentTimeMillis();
            // 格式化时间字符串
            String startTimeStr = String.format("%tF %<tT", startTime);
            OpenAiChatRequestParameters params = OpenAiChatRequestParameters.builder()
                .customParameters(Map.of(
                        "chat_template_kwargs", Map.of(
                            "enable_thinking", false
                        ),
                        "enable_thinking", false
                    )
                )
                .build();
            ChatRequest chatRequest = ChatRequest.builder().parameters(params)
                .messages(systemMessage, userMessage)
                .build();

            logger.info("知识图谱任务 +1.");
            ChatResponse response = model.chat(chatRequest);

            long endTime = System.currentTimeMillis();
            // 格式化时间字符串
            String endTimeStr = String.format("%tF %<tT", endTime);

            String text = response.aiMessage().text();
            long costTime = endTime - startTime;
            logger.info("systemMessage：{}\n userMessage：{}\n答复：{}\n开始时间：{}, 结束时间：{}, 耗时：{}ms, 输入token数：{}, 输出token数：{}",
                systemMessage,userMessage, text, startTimeStr, endTimeStr, costTime, response.tokenUsage().inputTokenCount(), response.tokenUsage().outputTokenCount());

            this.text = response.aiMessage().text();
            return text;
        } catch (Throwable t) {
            return null;
        }
    }
}