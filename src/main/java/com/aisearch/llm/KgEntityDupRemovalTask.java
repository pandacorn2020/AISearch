package com.aisearch.llm;

import com.aisearch.entity.KGEntity;
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

public class KgEntityDupRemovalTask implements Callable<KGEntity> {
    private String dupRemovalTemplate;

    private KGEntity entity;

    private ChatModel model;

    private static final Logger logger = LoggerFactory.getLogger(KgEntityDupRemovalTask.class.getSimpleName());

    public KgEntityDupRemovalTask(String dupRemovalTemplate,
                                  KGEntity entity,
                                  ChatModel model) {
        this.dupRemovalTemplate = dupRemovalTemplate;
        this.entity = entity;
        this.model = model;
    }

    @Override
    public KGEntity call() {
        try {
            String message = String.format(dupRemovalTemplate, entity.getDescription());
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
                .messages(userMessage)
                .build();

            logger.info("实体去重任务 +1.");
            ChatResponse response = model.chat(chatRequest);
            long endTime = System.currentTimeMillis();
            // 格式化时间字符串
            String endTimeStr = String.format("%tF %<tT", endTime);

            String text = response.aiMessage().text();
            // 将 response.tokenUsage() 追加存储进一个指定的日志文件中，如果文件没有自动创建，文件有了，则追加存储
            long costTime = endTime - startTime;



            logger.info("systemMessage：{}\n userMessage：{}\n答复：{}\n开始时间：{}, 结束时间：{}, 耗时：{}ms, 输入token数：{}, 输出token数：{}",
                "",userMessage, text, startTimeStr, endTimeStr, costTime, response.tokenUsage().inputTokenCount(), response.tokenUsage().outputTokenCount());
            entity.setDescription(text);
            return entity;
        } catch (Throwable t) {
            return null;
        }
    }
}