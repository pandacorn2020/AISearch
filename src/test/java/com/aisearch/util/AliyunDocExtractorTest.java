package com.aisearch.util;

import com.aisearch.config.KgProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootTest
public class AliyunDocExtractorTest {

    /** 测试用 PDF 路径 */
    private static final String TEST_PDF = "src/test/resources/china-economic-monitor-q2-2024.pdf";

    @Autowired
    private KgProperties kgProperties;

    @Test
    public void testExtract() throws Exception {
        Path pdfPath = Paths.get(TEST_PDF);
        if (!Files.exists(pdfPath)) {
            System.out.println("测试 PDF 不存在，跳过: " + pdfPath.toAbsolutePath());
            return;
        }

        byte[] pdfBytes = Files.readAllBytes(pdfPath);
        System.out.println("PDF 文件大小: " + pdfBytes.length + " bytes");
        System.out.println("PDF 前4字节 (hex): " + bytesToHex(pdfBytes, 4));

        AliyunDocExtractor extractor = new AliyunDocExtractor(kgProperties);
        String result = extractor.extract(pdfBytes, pdfPath.getFileName().toString());

        System.out.println("=== 提取结果 (前500字符) ===");
        if (result != null && !result.isEmpty()) {
            System.out.println(result.substring(0, Math.min(500, result.length())));
            System.out.println("=== 总字符数: " + result.length() + " ===");
        } else {
            System.out.println("(空)");
        }
    }

    /** 测试扫描版 PDF */
    @Test
    public void testExtractScannedPdf() {
        Path pdfPath = Paths.get("D:/tmp/aisearch/陈勇实用验方选.pdf");
        if (!Files.exists(pdfPath)) {
            System.out.println("扫描版 PDF 不存在，跳过: " + pdfPath.toAbsolutePath());
            return;
        }

        try {
            byte[] pdfBytes = Files.readAllBytes(pdfPath);
            System.out.println("扫描版 PDF 大小: " + pdfBytes.length + " bytes");
            System.out.println("扫描版 PDF 前4字节 (hex): " + bytesToHex(pdfBytes, 4));

            AliyunDocExtractor extractor = new AliyunDocExtractor(kgProperties);
            String result = extractor.extract(pdfBytes, pdfPath.getFileName().toString());
            System.out.println("result: " + result);
            System.out.println("提取成功（意外），总字符数: " + (result != null ? result.length() : 0));
        } catch (Exception e) {
            System.out.println("预期中的失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /** 测试传入空字节数组 */
    @Test
    public void testExtractEmptyBytes() {
        try {
            AliyunDocExtractor extractor = new AliyunDocExtractor(kgProperties);
            extractor.extract(new byte[0], "empty.pdf");
            System.out.println("FAIL: 应该抛出异常");
        } catch (IllegalArgumentException e) {
            System.out.println("OK: 空字节数组正确抛出异常: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("异常类型不匹配: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    private static String bytesToHex(byte[] bytes, int maxLen) {
        StringBuilder sb = new StringBuilder();
        int len = Math.min(bytes.length, maxLen);
        for (int i = 0; i < len; i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%02X", bytes[i] & 0xFF));
        }
        return sb.toString();
    }
}
