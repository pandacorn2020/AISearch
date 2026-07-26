package com.aisearch.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileConverter {

    private static final Logger log = LoggerFactory.getLogger(FileConverter.class);

    public static Path getResourcePath(String resourceFileName) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourceFileName);
        return Paths.get(resource.getURI());
    }

    public static String convertFileToText(InputStream inputStream, String fileName) throws IOException {
        if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
            return convertWordToText(inputStream, fileName);
        } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
            return convertExcelToText(inputStream, fileName);
        } else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
            return convertPptToText(inputStream, fileName);

        }else {
            // read input stream as text
            return new String(inputStream.readAllBytes(), "UTF-8");
        }
    }

    public static String convertWordToText(InputStream inputStream, String fileName) throws IOException {
        // 先缓冲到内存，以便在 .doc/.docx 格式间降级重试
        byte[] data = inputStream.readAllBytes();

        if (fileName.endsWith(".doc")) {
            try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(data))) {
                WordExtractor extractor = new WordExtractor(document);
                return extractor.getText();
            } catch (Exception e1) {
                // 可能是 .docx 重命名为 .doc，尝试 .docx 格式
                try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(data))) {
                    XWPFWordExtractor extractor = new XWPFWordExtractor(document);
                    return extractor.getText();
                } catch (Exception e2) {
                    // 两种格式都失败，降级使用 Tika 自动检测真实格式
                    log.warn("文件 {} 作为 .doc(HWPF) 和 .docx(XWPF) 均解析失败，降级 Tika 检测。POI 异常: HWPF={}, XWPF={}",
                            fileName, e1.getMessage(), e2.getMessage());
                    return tikaConvertWordToText(new ByteArrayInputStream(data), fileName);
                }
            }
        } else if (fileName.endsWith(".docx")) {
            try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(data))) {
                XWPFWordExtractor extractor = new XWPFWordExtractor(document);
                return extractor.getText();
            } catch (Exception e1) {
                // 可能是旧格式 .doc 重命名为 .docx（二进制 OLE2，非 ZIP），降级尝试
                try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(data))) {
                    WordExtractor extractor = new WordExtractor(document);
                    return extractor.getText();
                } catch (Exception e2) {
                    // 两种格式都失败，降级使用 Tika 自动检测真实格式
                    log.warn("文件 {} 作为 .docx(XWPF) 和 .doc(HWPF) 均解析失败，降级 Tika 检测。POI 异常: XWPF={}, HWPF={}",
                            fileName, e1.getMessage(), e2.getMessage());
                    return tikaConvertWordToText(new ByteArrayInputStream(data), fileName);
                }
            }
        } else {
            throw new IllegalArgumentException("The specified file is not a Word document");
        }
    }

    public static String tikaConvertWordToText(InputStream inputStream, String fileName) throws IOException {
        try {
            Tika tika = new Tika();
            // 先缓冲数据用于类型检测
            byte[] data = inputStream.readAllBytes();
            String detectedType = tika.detect(data, fileName);
            log.info("Tika 兜底解析文件: {}，检测到实际格式: {}", fileName, detectedType);
            // 提取文本
            return tika.parseToString(new ByteArrayInputStream(data));
        } catch (Exception e) {
            log.error("Tika 兜底解析失败: {}", fileName, e);
            return null;
        }
    }

    public static String convertExcelToText(InputStream inputStream, String fileName) throws IOException {
        StringBuilder text = new StringBuilder();
        try (Workbook workbook = fileName.endsWith(".xls") ? new HSSFWorkbook(inputStream) : new XSSFWorkbook(inputStream)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        text.append(cell.toString()).append("\t");
                    }
                    text.append("\n");
                }
            }
        }
        return text.toString();
    }

    public static String convertExcelToJson(Path excelFilePath) throws IOException{
        List<Map<String, String>> sheetData = new ArrayList<>();
        InputStream inputStream = Files.newInputStream(excelFilePath);
        try (InputStream fis = Files.newInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // Read the first sheet
            Row headerRow = sheet.getRow(0); // Assume the first row contains headers

            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Iterate through rows
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, String> rowData = new HashMap<>();
                for (int j = 0; j < headerRow.getLastCellNum(); j++) { // Iterate through columns
                    Cell headerCell = headerRow.getCell(j);
                    Cell cell = row.getCell(j);

                    String header = headerCell != null ? headerCell.getStringCellValue() : "Column" + j;
                    String value = cell != null ? getCellValueAsString(cell) : "";

                    rowData.put(header, value);
                }
                sheetData.add(rowData);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(sheetData); // Convert to JSON string
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getCellValueAsString(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    public static String convertPptToText(InputStream inputStream, String fileName) throws IOException {
        if (fileName.endsWith(".pptx")) {
            // PPTX 文件
            try (XMLSlideShow ppt = new XMLSlideShow(inputStream)) {
                StringBuilder sb = new StringBuilder();
                int slideNum = 1;

                for (var slide : ppt.getSlides()) {
                    sb.append("=== Slide ").append(slideNum).append(" ===\n");

                    for (XSLFShape shape : slide.getShapes()) {
                        if (shape instanceof XSLFTextShape) {
                            String text = ((XSLFTextShape) shape).getText();
                            if (text != null && !text.isBlank()) {
                                sb.append(text).append("\n");
                            }
                        }
                    }

                    slideNum++;
                }

                return sb.toString();
            }
        } else if (fileName.endsWith(".ppt")) {
            // PPT 老格式
            try (HSLFSlideShow ppt = new HSLFSlideShow(inputStream)) {
                StringBuilder sb = new StringBuilder();
                int slideNum = 1;

                for (var slide : ppt.getSlides()) {
                    sb.append("=== Slide ").append(slideNum).append(" ===\n");

                    for (HSLFShape shape : slide.getShapes()) {
                        if (shape instanceof HSLFTextShape) {
                            String text = ((HSLFTextShape) shape).getText();
                            if (text != null && !text.isBlank()) {
                                sb.append(text).append("\n");
                            }
                        }
                    }

                    slideNum++;
                }

                return sb.toString();
            }
        } else {
            throw new IllegalArgumentException("The specified file is not a PPT document");
        }
    }


    public static void main(String[] args) {
        try {
            Path resourcePath = Paths.get("C:/temp/维生素缺乏症.xlsx");
            String jsonOutput = convertExcelToJson(resourcePath);
            System.out.println(jsonOutput);
            Files.writeString(Paths.get("C:/temp/维生素缺乏症.json"), jsonOutput);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}