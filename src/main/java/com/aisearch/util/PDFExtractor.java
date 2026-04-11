package com.aisearch.util;

import com.aisearch.config.KgProperties;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aisearch.repository.JdbcRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.*;

public class PDFExtractor {

    private static final Logger logger = LoggerFactory.getLogger(PDFExtractor.class.getSimpleName());

    private static final String DEFAULT_OCR_PROMPT = "请识别图片中的全部文字，按原有换行输出。";

    private static final String DEFAULT_OCR_MODEL = "qwen2.5-vl-7b-instruct";

    private JdbcRepository jdbcRepository;
    private String schema;

    private final KgProperties kgProperties;

    private final HttpClient httpClient;

    public PDFExtractor(JdbcRepository jdbcRepository, String schema) {
        this(jdbcRepository, schema, null);
    }

    public PDFExtractor(JdbcRepository jdbcRepository, String schema, KgProperties kgProperties) {
        this.jdbcRepository = jdbcRepository;
        this.schema = schema;
        this.kgProperties = kgProperties;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    public String extract(InputStream inputStream) throws IOException {
        List<String> pageTexts = new ArrayList<>();
        try (PDDocument document = PDDocument.load(inputStream)) {
            // First pass: collect all text positions
            PDFPageTextWithLocationExtractor stripper = new PDFPageTextWithLocationExtractor();
            // Second pass: extract images with context
            int pageCount = document.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                String pageText = stripper.getText(document);
                pageTexts.add(pageText.trim()); // 去除前后空格后加入列表
                PDPage page = document.getPage(i);
                float pageHeight = page.getMediaBox().getHeight();
                List<TextPosition> textPositions = stripper.getTextPositions();
                PDFImageProcessEngine imageProcessEngine = new PDFImageProcessEngine(i, pageCount, pageHeight, textPositions,
                    pageText, jdbcRepository, schema);
                imageProcessEngine.processPage(page);
            }

            if (isAllImagePdf(pageTexts)) {
                logger.info("检测到纯图片 PDF，切换到 OCR 流程。schema={}", schema);
                List<String> ocrTexts = extractByOcr(document);
                if (!ocrTexts.isEmpty() && ocrTexts.stream().anyMatch(PDFExtractor::hasText)) {
                    pageTexts = ocrTexts;
                } else {
                    logger.warn("OCR 流程返回空内容，回退到原始提取文本。");
                }
            }
        }
        StringJoiner joiner = new StringJoiner("\n");
        for (String pageText : pageTexts) {
            joiner.add(pageText);
        }
        return joiner.toString();
    }

    private boolean isAllImagePdf(List<String> pageTexts) {
        for (String pageText : pageTexts) {
            if (hasText(pageText)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private List<String> extractByOcr(PDDocument document) {
        List<String> ocrTexts = new ArrayList<>();
        if (!ocrEnabled()) {
            logger.warn("未启用 OCR：kg.ocrUrl 为空。");
            return ocrTexts;
        }
        try {
            PDFRenderer renderer = new PDFRenderer(document);
            int pages = document.getNumberOfPages();
            for (int i = 0; i < pages; i++) {
                try {
                    BufferedImage image = renderer.renderImageWithDPI(i, 200);
                    byte[] bytes = bufferedImageToPng(image);
                    String pageText = ocrImage(bytes, i + 1, pages);
                    ocrTexts.add(pageText == null ? "" : pageText.trim());
                } catch (Exception pageEx) {
                    logger.error("第 {}/{} 页 OCR 失败", i + 1, pages, pageEx);
                    ocrTexts.add("");
                }
            }
        } catch (Exception ex) {
            logger.error("OCR 流程执行失败", ex);
        }
        return ocrTexts;
    }

    private boolean ocrEnabled() {
        return kgProperties != null
            && kgProperties.getOcrUrl() != null
            && !kgProperties.getOcrUrl().trim().isEmpty();
    }

    private String ocrImage(byte[] imageBytes, int page, int pageCount) throws Exception {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String imageUrl = "data:image/png;base64," + base64;

        String model = DEFAULT_OCR_MODEL;
        if (kgProperties != null && kgProperties.getOcrModelName() != null
            && !kgProperties.getOcrModelName().trim().isEmpty()) {
            model = kgProperties.getOcrModelName().trim();
        }

        String prompt = DEFAULT_OCR_PROMPT;
        if (kgProperties != null && kgProperties.getOcrPrompt() != null
            && !kgProperties.getOcrPrompt().trim().isEmpty()) {
            prompt = kgProperties.getOcrPrompt().trim();
        }

        JSONObject textContent = new JSONObject();
        textContent.put("type", "text");
        textContent.put("text", prompt);

        JSONObject imageUrlObj = new JSONObject();
        imageUrlObj.put("url", imageUrl);

        JSONObject imageContent = new JSONObject();
        imageContent.put("type", "image_url");
        imageContent.put("image_url", imageUrlObj);

        JSONArray content = new JSONArray();
        content.add(textContent);
        content.add(imageContent);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", content);

        JSONArray messages = new JSONArray();
        messages.add(userMessage);

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", messages);
        body.put("ocr_mode", true);
        body.put("temperature", 0);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(kgProperties.getOcrUrl()))
            .timeout(Duration.ofSeconds(120))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString(), StandardCharsets.UTF_8));

        String apiKey = null;
        if (kgProperties != null && kgProperties.getOcrApiKey() != null
            && !kgProperties.getOcrApiKey().trim().isEmpty()) {
            apiKey = kgProperties.getOcrApiKey().trim();
        }
        if (apiKey != null) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> response = httpClient.send(
            requestBuilder.build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() / 100 != 2) {
            throw new RuntimeException("OCR http status=" + response.statusCode()
                + ", body=" + response.body());
        }

        String text = parseOcrText(response.body());
        int len = Math.min(128, text == null ? 0 : text.length());
        if (len > 0) {
            logger.info("OCR 第 {}/{} 页，文本={}", page, pageCount, text.substring(0, len));
        } else {
            logger.info("OCR 第 {}/{} 页，文本为空", page, pageCount);
        }
        return text;
    }

    private String parseOcrText(String body) {
        JSONObject json = JSON.parseObject(body);
        if (json == null) {
            return "";
        }
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        JSONObject first = choices.getJSONObject(0);
        if (first == null) {
            return "";
        }
        JSONObject message = first.getJSONObject("message");
        if (message == null) {
            return "";
        }
        Object content = message.get("content");
        if (content == null) {
            return "";
        }
        if (content instanceof String) {
            return ((String) content).trim();
        }
        if (content instanceof JSONArray) {
            StringBuilder sb = new StringBuilder();
            JSONArray arr = (JSONArray) content;
            for (int i = 0; i < arr.size(); i++) {
                JSONObject part = arr.getJSONObject(i);
                if (part == null) {
                    continue;
                }
                String partText = part.getString("text");
                if (partText != null) {
                    sb.append(partText);
                }
            }
            return sb.toString().trim();
        }
        return String.valueOf(content).trim();
    }

    private static byte[] bufferedImageToPng(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        baos.flush();
        return baos.toByteArray();
    }

}