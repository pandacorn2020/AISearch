package com.aisearch.util;

import com.aisearch.repository.JdbcRepository;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class PDFExtractor {

    private JdbcRepository jdbcRepository;
    private String schema;

    public PDFExtractor(JdbcRepository jdbcRepository, String schema) {
        this.jdbcRepository = jdbcRepository;
        this.schema = schema;
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
        }
        StringJoiner joiner = new StringJoiner("\n");
        for (String pageText : pageTexts) {
            joiner.add(pageText);
        }
        return joiner.toString();
    }

}