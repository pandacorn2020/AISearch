package com.aisearch.util;

import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.state.*;
import org.apache.pdfbox.contentstream.operator.text.*;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PDFImageTextExtractor {

    private final Map<Integer, List<TextPosition>> pageTextPositions = new HashMap<>();
    private int currentPage = -1;
    private Matrix currentTransformationMatrix = new Matrix();
    private final String outputDir;

    public PDFImageTextExtractor(String outputDir) throws IOException {
        this.outputDir = outputDir;
    }

    public void extract(File pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            // First pass: collect all text positions
            extractTextPositions(document);

            // Second pass: extract images with context
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                currentPage = i;
                currentTransformationMatrix = new Matrix();
                // processPage(document.getPage(i));
                PDPage page = document.getPage(i);
                float pageHeight = page.getMediaBox().getHeight();
                List<TextPosition> textPositions = pageTextPositions.getOrDefault(i, Collections.emptyList());
                PDFImageProcessEngine imageProcessEngine = new PDFImageProcessEngine(i, pageHeight, textPositions);
                imageProcessEngine.processPage(page);
            }
        }
    }

    private void extractTextPositions(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                int page = getCurrentPageNo() - 1;
                pageTextPositions.computeIfAbsent(page, k -> new ArrayList<>()).addAll(textPositions);
                super.writeString(text, textPositions);
            }
        };
        stripper.getText(document);
    }


    private float getFloatValue(COSBase base) {
        if (base instanceof COSFloat) {
            return ((COSFloat) base).floatValue();
        } else if (base instanceof COSInteger) {
            return ((COSInteger) base).floatValue();
        }
        return 0;
    }

    public static void main(String[] args) throws IOException {
        String pdfPath = "/input/input5.pdf";
        String outputDir = "/output/";

        new File(outputDir).mkdirs();
        PDFImageTextExtractor extractor = new PDFImageTextExtractor(outputDir);
        extractor.extract(new File(pdfPath));
    }
}