package com.aisearch.util;

import com.aliyun.docmind_api20220711.Client;
import com.aliyun.docmind_api20220711.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.aisearch.config.KgProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

/**
 * 阿里云文档智能（DocMind）——文档解析（大模型版）提取器。
 *
 * <p>使用文档解析（大模型版）API，支持文字型 PDF、扫描版 PDF、Word、图片等多种格式。</p>
 * <p>工作流：SubmitDocParserJobAdvance → QueryDocParserStatus（轮询）→ GetDocParserResult</p>
 *
 * <p>参考官方文档：https://help.aliyun.com/zh/document-mind/developer-reference/document-parsing-large-model-version</p>
 */
public class AliyunDocExtractor {

    private static final Logger logger = LoggerFactory.getLogger(AliyunDocExtractor.class);

    /** 轮询间隔（毫秒） */
    private static final long POLLING_DELAY_MS = 5000L;

    /** 最大轮询次数（5s * 1440 = 120 分钟） */
    private static final int MAX_POLL_COUNT = 1440;

    /** 单次拉取 layout 最大数量 */
    private static final int LAYOUT_STEP_SIZE = 3000;

    private final Client client;
    private final RuntimeOptions runtime;

    public AliyunDocExtractor(KgProperties kgProperties) throws Exception {
        String accessKey = kgProperties.getAliDocAccessKey();
        String secretKey = kgProperties.getAliDocSecretKey();
        String endpoint = kgProperties.getAliDocEndpoint();

        if (accessKey == null || accessKey.trim().isEmpty()) {
            throw new IllegalArgumentException("阿里云文档智能 AccessKey 未配置（kg.aliDocAccessKey）");
        }
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalArgumentException("阿里云文档智能 SecretKey 未配置（kg.aliDocSecretKey）");
        }

        Config config = new Config()
            .setAccessKeyId(accessKey.trim())
            .setAccessKeySecret(secretKey.trim())
            .setEndpoint(endpoint);
        this.client = new Client(config);
        this.runtime = new RuntimeOptions();
        logger.info("阿里云文档智能（大模型版）客户端初始化成功，endpoint={}", endpoint);
    }

    /**
     * 提取 PDF 文件的全部文本。
     *
     * @param pdfBytes PDF 文件字节数组
     * @param fileName 文件名（用于提交任务时的标识）
     * @return 提取的 Markdown 文本
     */
    public String extract(byte[] pdfBytes, String fileName) throws Exception {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF 字节数组为空，fileName=" + fileName);
        }
        logger.info("阿里云文档智能（大模型版）接收文件，fileName={}, 大小={} bytes",
            fileName, pdfBytes.length);

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("aliyun_doc_", "_" + sanitizeFileName(fileName));
            Files.write(tempFile, pdfBytes);

            // 1. 提交文档解析（大模型版）任务
            String jobId = submitJob(tempFile.toFile(), fileName);
            logger.info("任务已提交，jobId={}, fileName={}", jobId, fileName);

            // 2. 轮询等待处理完成
            pollForCompletion(jobId);

            // 3. 获取结果（Markdown 格式）
            String markdown = fetchResult(jobId);
            logger.info("解析完成，jobId={}, 总字符数={}", jobId, markdown != null ? markdown.length() : 0);
            return markdown != null ? markdown : "";

        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    logger.warn("删除临时文件失败: {}", tempFile, e);
                }
            }
        }
    }

    /**
     * 提交本地文件异步解析任务（SubmitDocParserJobAdvance）。
     */
    private String submitJob(File file, String fileName) throws Exception {
        SubmitDocParserJobAdvanceRequest request = new SubmitDocParserJobAdvanceRequest();
        request.fileName = fileName;
        // 输出 Markdown 格式，方便直接获取文本
        request.outputFormat = Collections.singletonList("markdown");

        try (FileInputStream fis = new FileInputStream(file)) {
            request.fileUrlObject = fis;
            SubmitDocParserJobResponse response = client.submitDocParserJobAdvance(request, runtime);
            if (response.getStatusCode() != 200) {
                throw new RuntimeException("提交文档解析任务失败，HTTP状态码: " + response.getStatusCode());
            }
            if (response.getBody() == null || response.getBody().getData() == null) {
                throw new RuntimeException("提交文档解析任务返回为空");
            }
            // Data 对象中的 Id 字段
            String id = response.getBody().getData().getId();
            if (id == null || id.isEmpty()) {
                throw new RuntimeException("提交返回中无 Id 字段");
            }
            return id;
        }
    }

    /**
     * 轮询 QueryDocParserStatus 直到任务完成。
     */
    @SuppressWarnings("unchecked")
    private void pollForCompletion(String jobId) throws Exception {
        for (int i = 0; i < MAX_POLL_COUNT; i++) {
            QueryDocParserStatusRequest request = new QueryDocParserStatusRequest();
            request.id = jobId;
            QueryDocParserStatusResponse response = client.queryDocParserStatus(request);

            if (response.getBody() == null) {
                throw new RuntimeException("查询解析状态返回为空，jobId=" + jobId);
            }

            // 使用 toMap() 获取所有字段，兼容 SDK 版本差异
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> bodyMap = (java.util.Map<String, Object>) response.getBody().toMap();

            // 响应结构: Body { Code, Data:{Status}, Message, RequestId }
            Object dataObj = bodyMap.get("Data");
            String status = "";
            if (dataObj instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) dataObj;
                status = String.valueOf(dataMap.getOrDefault("Status", ""));
            }
            String code = String.valueOf(bodyMap.getOrDefault("Code", ""));

            if ("success".equalsIgnoreCase(status)) {
                logger.info("任务处理成功，jobId={}, 轮询次数={}", jobId, i + 1);
                return;
            }
            if ("fail".equalsIgnoreCase(status)) {
                throw new RuntimeException("阿里云文档智能任务失败，jobId=" + jobId
                    + ", code=" + code
                    + ", message=" + bodyMap.get("Message"));
            }

            // Init / Processing —— 继续轮询
            try {
                Thread.sleep(POLLING_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("轮询被中断，jobId=" + jobId, e);
            }
        }
        throw new RuntimeException("阿里云文档智能任务超时（" + (MAX_POLL_COUNT * POLLING_DELAY_MS / 1000) + "秒），jobId=" + jobId);
    }

    /**
     * 获取解析结果（GetDocParserResult），提取 layouts[].markdownContent 拼接纯文本。
     */
    @SuppressWarnings("unchecked")
    private String fetchResult(String jobId) throws Exception {
        StringBuilder sb = new StringBuilder();

        GetDocParserResultRequest request = new GetDocParserResultRequest();
        request.id = jobId;
        request.layoutStepSize = LAYOUT_STEP_SIZE;
        request.layoutNum = 0;

        GetDocParserResultResponse response = client.getDocParserResult(request);

        if (response.getBody() == null) {
            return "";
        }

        java.util.Map<String, Object> bodyMap = (java.util.Map<String, Object>) response.getBody().toMap();

        // Data 已在 toMap() 中展开为 Map: { layouts: [{ markdownContent: "...", ... }, ...] }
        Object dataObj = bodyMap.get("Data");
        if (!(dataObj instanceof java.util.Map)) {
            return "";
        }

        java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) dataObj;
        Object layoutsObj = dataMap.get("layouts");
        if (!(layoutsObj instanceof java.util.List)) {
            return "";
        }

        java.util.List<?> layouts = (java.util.List<?>) layoutsObj;
        for (Object item : layouts) {
            if (item instanceof java.util.Map) {
                java.util.Map<String, Object> layout = (java.util.Map<String, Object>) item;
                Object md = layout.get("markdownContent");
                if (md != null) {
                    sb.append(md.toString());
                }
            }
        }

        return sb.toString();
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) {
            return "document.pdf";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
