package com.aisearch.util;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PDFPageTextWithLocationExtractorTest {

    @Test
    void testGetContent() throws IOException {
        // Arrange: Load a sample PDF file
        File pdfFile = new File("/input/现代舰船.pdf");
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFPageTextWithLocationExtractor extractor = new PDFPageTextWithLocationExtractor();

            int numberOfPages = document.getNumberOfPages();
            for (int i = 1; i <= numberOfPages; i++) {
                extractor.setStartPage(i);
                extractor.setEndPage(i);
                String pageText = extractor.getText(document);
                System.out.println("Page " + i + " text: " + pageText);
            }
            String content = extractor.getContent();
            System.out.println("Extracted content: " + content);
        }
    }
}