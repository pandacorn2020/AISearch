package com.aisearch.llm;

import com.aisearch.entity.KGRelationship;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.output.Response;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Callable;

public class KgRelationshipDupRemovalTask implements Callable<KGRelationship> {
    private String dupRemovalTemplate;

    private KGRelationship relationship;

    private ChatModel model;

    private static final Logger logger = LoggerFactory.getLogger(KgRelationshipDupRemovalTask.class.getSimpleName());


    public KgRelationshipDupRemovalTask(String dupRemovalTemplate,
                                        KGRelationship relationship,
                                  ChatModel model) {
        this.dupRemovalTemplate = dupRemovalTemplate;
        this.relationship = relationship;
        this.model = model;
    }

    @Override
    public KGRelationship call() {
        try {
            String message = String.format(dupRemovalTemplate,
                    relationship.getDescription());

            ChatMessage userMessage = UserMessage.userMessage(message);
            FileUtils.writeStringToFile(new File("token_usage.log"), "request user message:" + message + "\n", StandardCharsets.UTF_8, true);
            // 日志存储请求发起的时间和结束的时间，并算耗时
            long startTime = System.currentTimeMillis();
            // 格式化时间字符串
            String startTimeStr = String.format("%tF %<tT", startTime);

            OpenAiChatRequestParameters params = OpenAiChatRequestParameters.builder()
                .customParameters(Map.of("enable_thinking", false))
                .build();
            ChatRequest chatRequest = ChatRequest.builder().parameters(params)
                .messages( userMessage)
                .build();

            logger.info("关系去重任务 +1.");
            ChatResponse response = model.chat(chatRequest);

            long endTime = System.currentTimeMillis();
            // 格式化时间字符串
            String endTimeStr = String.format("%tF %<tT", endTime);

            String text = response.aiMessage().text();
            long costTime = endTime - startTime;


            logger.info("systemMessage：{}\n userMessage：{}\n答复：{}\n开始时间：{}, 结束时间：{}, 耗时：{}ms, 输入token数：{}, 输出token数：{}",
                "",userMessage, text, startTimeStr, endTimeStr, costTime, response.tokenUsage().inputTokenCount(), response.tokenUsage().outputTokenCount());
            relationship.setDescription(text);
            return relationship;
        } catch (Throwable t) {
            return null;
        }
    }
}