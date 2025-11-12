package com.aisearch.llm;

import com.aisearch.entity.KGRelationship;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

public class KgRelationshipDupRemovalTask implements Callable<KGRelationship> {
    private String dupRemovalTemplate;

    private KGRelationship relationship;

    private ChatLanguageModel model;

    public KgRelationshipDupRemovalTask(String dupRemovalTemplate,
                                        KGRelationship relationship,
                                  ChatLanguageModel model) {
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
            FileUtils.writeStringToFile(new File("token_usage.log"), "request time:" + startTimeStr + "\n", StandardCharsets.UTF_8, true);
            System.out.println("请求开始了：");
            Response<AiMessage> response = model.generate(userMessage);
            System.out.println("请求结束了。");
            long endTime = System.currentTimeMillis();
            // 格式化时间字符串
            String endTimeStr = String.format("%tF %<tT", endTime);
            FileUtils.writeStringToFile(new File("token_usage.log"), "response time:" + endTimeStr + "\n", StandardCharsets.UTF_8, true);

            String text = response.content().text();
            // 将 response.tokenUsage() 追加存储进一个指定的日志文件中，如果文件没有自动创建，文件有了，则追加存储
            FileUtils.writeStringToFile(new File("token_usage.log"), "response:" + text + "\n", StandardCharsets.UTF_8, true);
            FileUtils.writeStringToFile(new File("token_usage.log"), "token usage:" + response.tokenUsage().toString() + "\n", StandardCharsets.UTF_8, true);
            long costTime = endTime - startTime;
            System.out.println("cost time: " + costTime + "ms");
            // 写入到日志
            FileUtils.writeStringToFile(new File("token_usage.log"), "cost time:" + costTime + "ms\n", StandardCharsets.UTF_8, true);
            System.out.println(response.tokenUsage());

            relationship.setDescription(text);
            return relationship;
        } catch (Throwable t) {
            return null;
        }
    }
}