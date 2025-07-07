package com.aisearch.util;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PDFPageTextWithLocationExtractor extends PDFTextStripper {

    private List<String> pageTexts = new ArrayList<>();
    private StringBuilder currentPageText = new StringBuilder();
    private List<TextPosition> textPositions = new ArrayList<>();

    public PDFPageTextWithLocationExtractor() throws IOException {
        super();
    }

    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        super.writeString(string, textPositions);
        this.textPositions.addAll(textPositions);
        currentPageText.append(string);
    }

    @Override
    protected void startPage(PDPage page) throws IOException{
        super.startPage(page);
        currentPageText.setLength(0); // 清空当前页内容
        textPositions.clear(); // 清空位置信息
    }

    @Override
    protected void endPage(PDPage page) throws IOException {
        super.endPage(page);
        pageTexts.add(currentPageText.toString());
    }

    public List<String> getPageTexts() {
        return pageTexts;
    }

    public String getContent() {
        return String.join("\n", pageTexts);
    }

    public List<TextPosition> getTextPositions() {
        return textPositions;
    }

}