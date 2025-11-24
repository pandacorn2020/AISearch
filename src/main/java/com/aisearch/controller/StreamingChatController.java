package com.aisearch.controller;

import com.aisearch.dto.RequestCommon;
import com.aisearch.dto.RequestContentChatSelect;
import com.aisearch.dto.RequestContentKnowledgeSearch;
import com.aisearch.dto.ResponseCommon;
import com.aisearch.entity.KGImage;
import com.aisearch.repository.JdbcRepository;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aisearch.llm.*;
import com.aisearch.service.DocumentLoader;
import com.aisearch.service.GraphSearch;
import com.alibaba.fastjson.TypeReference;
import com.wisdomdata.jdbc.CloudConnection;
import com.wisdomdata.jdbc.CloudStatement;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.aisearch.controller.KnowledgeController.isRequestBodyValid;
import static com.wisdomdata.jdbc.CloudStatement.END_OF_STREAMING_CHAT;
import static dev.langchain4j.data.message.SystemMessage.systemMessage;

@CrossOrigin(origins = "*")
@RestController
@SessionAttributes(value = {"statement", "connection"})
@RequestMapping("/aisearch")
public class StreamingChatController {

    private static CloudConnection conn;
    private static CloudStatement stmt;
    private static final Logger logger = java.util.logging.Logger.getLogger(StreamingChatController.class.getSimpleName());


    private static ConcurrentHashMap<String, HashMap<String, Object>> mapUserData = new ConcurrentHashMap<>();

    private static HashMap<String, String[]> mapChatHistory = new HashMap<>();


    private static String KEY_HISTORY = "history";

    private static String KEY_INPUT = "input";
    private static String KEY_ENTITIES = "entities";
    private static String KEY_KEY_SCHEMA = "key_schema";
    private static String KEY_KEY_SCHEMA_DESCRIPTION = "key_schema_description";

    public static String RAG_QUERY= "ragQuery";


    public static final String INPUT = "input";
    public static final String CHAT_HISTORY = "chatHistory";
    public static final String CHAT = "chat";
    public static final String SECONDARY = "secondary";
    public static final String HISTORY = "history";
    public static final String TOKEN = "token";
    public static final String ENTITIES = "entities";
    public static final String QUERY = "query";
    public static final String NEXT = "next";
    //    private static StreamingChatModel streamingChatModel;
//
//    private static String systemPrompt;
//
//    private static String secondaryPrompt;
//
    @Autowired
    private GraphSearch graphSearch;

    // create timed out cache
    private static final int CHAT_TIMEOUT_SECONDS = 300;

    @Autowired
    private ExecutorService executorService;  // 假设你有一个线程池

    @Autowired
    private LLMModel llmModel;


    @Autowired
    private DocumentLoader documentLoader;

    @Autowired
    private JdbcRepository jdbcRepository;


    interface Assistant {

        TokenStream chat(String userMessage);
    }

    @ResponseBody
    @PostMapping("/askgraph/select")
    public ResponseCommon<Object[]> askNumberSelect(@RequestBody String body, HttpSession session) throws SQLException, IOException {

        ResponseCommon.Message msg = new ResponseCommon.Message("", "");
        ResponseCommon<Object[]> resp = new ResponseCommon<>();
        resp.setMsg(msg);



        // 解析 JSON 为 RequestCommon<RequestContentKnowledgeReset>
        RequestCommon<RequestContentChatSelect> request;
        try {
            request = JSON.parseObject(
                body,
                new TypeReference<RequestCommon<RequestContentChatSelect>>() {}
            );
        } catch (Exception ex) {
            msg.setFail("请求体不是合法的 JSON 或解析失败: " + ex.getMessage());
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }

        // 打印（调试）
        System.out.println("收到请求: " + request);

        boolean requestBodyValid = isRequestBodyValid(request);
        if (!requestBodyValid) {
            msg.setFail("fromId,fromNickname,content 验证失败");
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }



        // 校验 schema 字段
        String schema = request.getContent().getSchema();
        if (!StringUtils.hasText(schema)) {
            msg.setFail("content.schema 为空");
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }


        Object o = mapUserData.get(request.getFromId());
        HashMap<String, Object> userData;
        if (o == null) {
            userData = new HashMap<>();
        } else {
            userData = mapUserData.get(request.getFromId());
        }
        userData.put(KEY_HISTORY, new ChatMessage[0]);
        userData.put(KEY_INPUT, "");
        userData.put(KEY_ENTITIES, new String[0]);
        userData.put(KEY_KEY_SCHEMA, request.getContent().getSchema());
        userData.put(KEY_KEY_SCHEMA_DESCRIPTION, request.getContent().getSchemaDescription());
        mapUserData.put(request.getFromId(), userData);

        resp.setCode(200);
        resp.setContent(new Object[]{});
        resp.setCount(0);
        resp.getMsg().setSuccess("success");
        return resp;

    }


    @GetMapping(value = "/image/{imageId}")
    public ResponseEntity<byte[]> getImageById(@PathVariable Long imageId) {
        try {
            // 获取图像字节和格式
            ImageWithFormat imageData = getImageBytesFromDatabase(imageId);

            if (imageData == null || imageData.bytes == null) {
                return ResponseEntity.notFound().build();
            }

            // 设置对应格式的 Content-Type
            MediaType mediaType = "png".equalsIgnoreCase(imageData.format)
                    ? MediaType.IMAGE_PNG
                    : MediaType.IMAGE_JPEG;

            return ResponseEntity
                    .ok()
                    .contentType(mediaType)
                    .body(imageData.bytes);

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // 根据 imageId 返回模拟图片（含格式信息）
    private ImageWithFormat getImageBytesFromDatabase(Long imageId) {
        try {

            KGImage kgimage = jdbcRepository.findKGImageById(imageId);
            return new ImageWithFormat(kgimage.getContent(), "png");
//
//            // 模拟图片格式判断逻辑
//            String format = imageId.toLowerCase().contains("png") ? "png" : "jpg";
//
//            int size = 10;
//            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
//            Graphics2D g2d = image.createGraphics();
//
//            // 设置颜色（让每个 imageId 生成不同颜色图）
//            Color color = imageId.hashCode() % 2 == 0 ? Color.BLUE : Color.ORANGE;
//            g2d.setColor(color);
//            g2d.fillRect(0, 0, size, size);
//            g2d.dispose();
//
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            ImageIO.write(image, format, baos);  // 根据 format 写入
//            return new ImageWithFormat(baos.toByteArray(), format);

        } catch (Exception e) {
            return null;
        }
    }

    // 小工具类：同时返回字节数组和图片格式
    private static class ImageWithFormat {
        byte[] bytes;
        String format;

        ImageWithFormat(byte[] bytes, String format) {
            this.bytes = bytes;
            this.format = format;
        }
    }

    @GetMapping(value = "/stream-graph-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGraphChat(@RequestParam String id, @RequestParam String query, @RequestParam String input, HttpSession session) throws SQLException {
        SseEmitter emitter = new SseEmitter(1200000L); // Timeout set to 1200 seconds
        emitter.onTimeout(() -> {
            logger.warning("Timeout occurred!");
            emitter.complete();
        });

        long startTime = System.currentTimeMillis();
        try {
            validateInputs(id,query, input);
            if (input.contains("介绍歼10")) {
                // 流式输出固定文字
                String Content = "\n" +
                        "# 歼-10“猛龙”战斗机\n" +
                        "\n" +
                        "> **英文名**：J-10\n" +
                        "> **绰号**：猛龙（Vigorous Dragon）\n" +
                        "\n" +
                        "---\n" +
                        "\n" +
                        "![歼-10战斗机](https://tmp-1301356618.cos.ap-beijing.myqcloud.com/data_lens/%E6%AD%BC10.jpg)\n" +
                        "\n" +
                        "## 基本简介\n" +
                        "\n" +
                        "歼-10（J-10）战斗机是中国自主研制的高性能、多用途、全天候第三代战斗机，具备完全自主知识产权。该机由中国空军编号为“歼-10”，对外称为 **J-10** 或 **F-10**。\n" +
                        "\n" +
                        "它具有以下显著特点：\n" +
                        "\n" +
                        "* **高可靠性、高生存力**\n" +
                        "* **高机动性能**\n" +
                        "* **作战半径大**\n" +
                        "* **起降距离短**\n" +
                        "* **攻击能力强**\n" +
                        "\n" +
                        "综合作战效能已达到国际同类先进战斗机的水平。\n" +
                        "\n" +
                        "---\n" +
                        "\n" +
                        "## 出口与国际合作\n" +
                        "\n" +
                        "### 巴基斯坦\n" +
                        "\n" +
                        "\uD83D\uDCC5 **2022年3月11日**\n" +
                        "巴基斯坦空军在卡姆拉基地举行了首批 **6架歼-10CE接装仪式**。\n" +
                        "\uD83C\uDDF5\uD83C\uDDF0 巴基斯坦总理伊姆兰·汗出席仪式。\n" +
                        "\n" +
                        "> 歼-10CE成为中国新一代航空主战装备首次实现**成体系、成建制出口**，标志着中国高端航空装备出口的新突破。\n" +
                        "\n" +
                        "---\n" +
                        "\n" +
                        "## 国际亮相\n" +
                        "\n" +
                        "\uD83D\uDCC5 **2024年8月28日**\n" +
                        "在首届埃及航展中，中国空军派出运-20与歼-10表演机编队，**飞越金字塔上空**，震撼亮相，展示中国航空工业实力。\n" +
                        "\n" +
                        "---\n";
                // 将Content每4个字分割成一个元素
                String[] fixedTexts = Content.split("(?<=\\G.{4})");
//                String[] fixedTexts = {"歼10照片", "歼10照片", "歼10照片"};
                for (String text : fixedTexts) {
                    emitter.send(text + "\n\n");
                    Thread.sleep(1000); // 每个文字之间暂停1秒
                }
                emitter.complete(); // 完成输出


            } else if (input.contains("歼10照片")) {
                // 流式输出固定文字
                String[] fixedTexts = {"![歼-10战斗机](https://tmp-1301356618.cos.ap-beijing.myqcloud.com/data_lens/%E6%AD%BC10.jpg)"};
                for (String text : fixedTexts) {
                    emitter.send(text + "\n\n");
                    Thread.sleep(1000); // 每个文字之间暂停1秒
                }
                emitter.complete(); // 完成输出
            } else {
                executeChat(id, input, query, emitter, startTime);
            }

        } catch (Exception e) {
            logger.warning("Error occurred: " + e.getMessage());
            logger.log( Level.SEVERE, "(line:342)LLM 调用发生错误: ", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private void validateInputs(String userId, String query, String input) throws Exception {
        if (userId == null || userId.trim().isEmpty() || input == null || input.trim().isEmpty() || query == null || query.trim().isEmpty()) {
            throw new Exception("userId, schema, inputText, or query is empty");
        }
    }

    private HashMap<String, Object> getUserData(String userId) {
        HashMap<String, Object> userData = mapUserData.get(userId);
        if (userData == null) {
            userData = new HashMap<>();
            mapUserData.put(userId, userData);
        }
        return userData;
    }

    private void executeChat(String userId, String input, String query, SseEmitter emitter, long startTime) {
        executorService.submit(() -> {
            try {
                HashMap<String, Object> userData = getUserData(userId);
                ChatMessage[] chatHistory = (ChatMessage[]) userData.get(KEY_HISTORY);

                String systemPrompt = documentLoader.readSystemPrompt();
                StreamingChatModel streamingChatModel = llmModel.buildStreamingModel();
                ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
                SystemMessage sysMessage = systemMessage(systemPrompt);

                setHistoryMessages(chatMemory, sysMessage, chatHistory);

                SessionData sessionData = new SessionData();
                sessionData.setUserData(userData);
                MeasureTools measureTools = new MeasureTools(sessionData, graphSearch, (String) userData.get(KEY_KEY_SCHEMA));

                StreamingChatController.Assistant assistant = AiServices.builder(StreamingChatController.Assistant.class)
                        .streamingChatModel(streamingChatModel)
                        .chatMemory(chatMemory)
                        .tools(measureTools)
                        .build();

                long beginLlmRequestStart = System.currentTimeMillis();
                StringBuilder reportString = new StringBuilder();

                assistant.chat(input)
                        .onPartialResponse(message -> handleOnNext(message, emitter, reportString))
                        .onCompleteResponse(aiMessageResponse -> handleOnComplete(emitter, chatMemory, userData, input,
                                startTime, beginLlmRequestStart, reportString.toString()))
                        .onError(throwable -> {
                            logger.warning("Error occurred: " + throwable.getMessage());
                            logger.log( Level.SEVERE, "(line:396)LLM 调用发生错误: ", throwable);

                            emitter.complete();
                        }).start();
            } catch (Exception e) {
                logger.warning("Error occurred: " + e.getMessage());
                logger.log( Level.SEVERE, "(line:402)LLM 调用发生错误: ", e);
                emitter.completeWithError(e);
            }
        });
    }

    private void handleOnNext(String message, SseEmitter emitter, StringBuilder reportString) {
        logger.info(message);
        reportString.append(message);
        if (message.equals(END_OF_STREAMING_CHAT)) {
            emitter.complete();
        }
        try {
            emitter.send(message + "\n\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleOnComplete(SseEmitter emitter, ChatMemory chatMemory, HashMap<String, Object> userData,
                                  String input, long startTime, long beginLlmRequestStart, String reportString) {
        emitter.complete();
        ChatMessage[] currentChatHistory = getChatHistory(chatMemory);
        userData.put(KEY_HISTORY, currentChatHistory);
        userData.put(KEY_INPUT, input);
        logger.info("Response time: " + (System.currentTimeMillis() - startTime) + "ms");
        logger.info("First token time: " + (System.currentTimeMillis() - beginLlmRequestStart) + "ms");
        logger.info("Report:\n" + reportString);
    }

    public static void setHistoryMessages(ChatMemory chatMemory,
                                          SystemMessage systemMessage, ChatMessage[] messages) {
        chatMemory.clear();
        if (systemMessage != null) {
            chatMemory.add(systemMessage);
        }
        for (ChatMessage message : messages) {
            chatMemory.add(message);
        }
    }

    private static ToolExecutionResultMessage buildToolExecutionResultMessage(String message) {
        int index = message.indexOf("id = ");
        int index1 = message.indexOf("toolName = ", index + 1);
        int index2 = message.indexOf("text = ", index1 + 1);
        String id = unquoted(message.substring(index + 6, index1 - 3).trim());
        String toolName = unquoted(message.substring(index1 + 11, index2 - 3).trim());
        String text = unquoted(message.substring(index2 + 8, message.length() - 2).trim());
        return new ToolExecutionResultMessage(id, toolName, text);
    }

    private static UserMessage buildUserMessage(String message) {
        int index = message.indexOf(" name = ");
        if (index > 0) {
            int index1 = message.indexOf(" contents = ", index + 1);
            String name = message.substring(index + 8, index1 - 1).trim();
            if (name.startsWith("\"") && name.endsWith("\"")) {
                name = name.substring(1, name.length() - 1);
            }
            String content = message.substring(index1 + 12, message.length() - 2).trim();
            content = content.substring(1, content.length() - 1);
            return UserMessage.userMessage(name, content);
        } else {
            return UserMessage.userMessage(message);
        }
    }

    private static AiMessage buildAiMessage(String message) {
        int index = message.indexOf("text = ");
        int index1 = message.indexOf("toolExecutionRequests = ", index + 1);
        String text = message.substring(index + 8, index1 - 3).trim();
        String tools = message.substring(index1 + 24, message.length() - 2).trim();
        if (tools.equals("null")) {
            return AiMessage.from(text);
        }
        index = tools.indexOf("ToolExecutionRequest {");
        List<ToolExecutionRequest> toolList = new ArrayList<>();
        while (index > 0) {
            int index2 = tools.indexOf("}", index + 1);
            String tool = tools.substring(index, index2 + 1);
            toolList.add(buildToolExecutionRequest(tool));
            index = tools.indexOf("ToolExecutionRequest {", index + 1);
        }
        return new AiMessage(text, toolList);
    }

    private static ToolExecutionRequest buildToolExecutionRequest(String tool) {
        int index = tool.indexOf("id = ");
        int index1 = tool.indexOf("name = ", index + 1);
        int index2 = tool.indexOf("arguments = ", index1 + 1);
        String id = unquoted(tool.substring(index + 6, index1 - 3).trim());
        String name = unquoted(tool.substring(index1 + 8, index2 - 3).trim());
        String arguments = unquoted(tool.substring(index2 + 13, tool.length() - 2).trim());
        return ToolExecutionRequest.builder().id(id).name(name).arguments(arguments).build();
    }

    private static String unquoted(String str) {
        if (str.equals("null")) {
            return null;
        }
        if (str.startsWith("'") && str.endsWith("'")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    private ChatMessage[] getChatHistory(ChatMemory chatMemory) {
        if (chatMemory == null) {
            return new ChatMessage[0];
        }
        List<ChatMessage> messages = chatMemory.messages();
        List<ChatMessage> history = new ArrayList();
        // 将messages转换为字符串数组

        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            if (message instanceof SystemMessage) {
                continue;
            }
            history.add(message);
        }
        return history.toArray(new ChatMessage[0]);
    }


}
