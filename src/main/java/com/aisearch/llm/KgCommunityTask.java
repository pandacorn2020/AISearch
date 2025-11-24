package com.aisearch.llm;

import com.aisearch.entity.CommunityData;
import com.aisearch.entity.KGEntity;
import com.aisearch.entity.KGGraph;
import com.aisearch.entity.KGRelationship;
import com.aisearch.service.SchemaService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.output.Response;
import org.apache.commons.io.FileUtils;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.Callable;

public class KgCommunityTask implements Callable<CommunityData> {
    private String communityTemplate;

    private KGGraph kgGraph;
    private Graph<KGEntity, KGRelationship> graph;

    private Set<KGEntity> cluster;

    private CommunityData communityData;

    private ChatModel model;

    private static final Logger logger = LoggerFactory.getLogger(KgCommunityTask.class.getSimpleName());

    public KgCommunityTask(String communityTemplate,
                           KGGraph kgGraph,
                           Graph<KGEntity, KGRelationship> graph,
                           Set<KGEntity> cluster,
                  ChatModel model) {
        this.communityTemplate = communityTemplate;
        this.kgGraph = kgGraph;
        this.graph = graph;
        this.cluster = cluster;
        this.model = model;
    }

    public CommunityData getCommunityData() {
        return communityData;
    }

    @Override
    public CommunityData call() {
        try {

            StringJoiner joiner = new StringJoiner("\n");
            StringJoiner edgeJoiner = new StringJoiner("\n");
            for (KGEntity node : cluster) {
                joiner.add(node.toString());
                for (KGRelationship edge : graph.edgesOf(node)) {
                    KGEntity source = kgGraph.getEntity(edge.getSource());
                    KGEntity target = kgGraph.getEntity(edge.getTarget());
                    if (cluster.contains(source) && cluster.contains(target)) {
                        edgeJoiner.add(edge.toString());
                    }
                }
            }
            String message = String.format(communityTemplate,
                    joiner.toString(), edgeJoiner.toString());

            ChatMessage userMessage = UserMessage.userMessage(message);
            logger.info("community detection request message: {}", message);
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

            logger.info("社区构建任务 +1.");
            ChatResponse response = model.chat(chatRequest);

            long endTime = System.currentTimeMillis();
            // 格式化时间字符串
            String endTimeStr = String.format("%tF %<tT", endTime);
            String text = response.aiMessage().text();
            long costTime = endTime - startTime;

            logger.info("systemMessage：{}\n userMessage：{}\n答复：{}\n开始时间：{}, 结束时间：{}, 耗时：{}ms, 输入token数：{}, 输出token数：{}",
                "",userMessage, text, startTimeStr, endTimeStr, costTime, response.tokenUsage().inputTokenCount(), response.tokenUsage().outputTokenCount());
            text = removeTag(text);
            this.communityData = CommunityData.read(text);
            return communityData;
        } catch (Throwable t) {
            return null;
        }
    }

    public static String removeTag(String text) {
        int startIndex = text.indexOf("```json");
        if (startIndex >= 0) {
            int endIndex = text.indexOf("```", startIndex + 1);
            text = text.substring(startIndex + "```json".length(), endIndex);
        }
        startIndex = text.indexOf("{");
        int index1 = text.indexOf("}");
        if (startIndex >= 0 && index1 >= 0 && index1 > startIndex) {
            return text.substring(startIndex, index1 + 1);
        }
        return text;
    }
}