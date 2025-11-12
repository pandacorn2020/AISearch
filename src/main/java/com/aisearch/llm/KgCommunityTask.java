package com.aisearch.llm;

import com.aisearch.entity.CommunityData;
import com.aisearch.entity.KGEntity;
import com.aisearch.entity.KGGraph;
import com.aisearch.entity.KGRelationship;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.apache.commons.io.FileUtils;
import org.jgrapht.Graph;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.Callable;

public class KgCommunityTask implements Callable<CommunityData> {
    private String communityTemplate;

    private KGGraph kgGraph;
    private Graph<KGEntity, KGRelationship> graph;

    private Set<KGEntity> cluster;

    private CommunityData communityData;

    private ChatLanguageModel model;

    public KgCommunityTask(String communityTemplate,
                           KGGraph kgGraph,
                           Graph<KGEntity, KGRelationship> graph,
                           Set<KGEntity> cluster,
                  ChatLanguageModel model) {
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