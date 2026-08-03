package com.aisearch.util;

import com.aisearch.repository.JdbcRepository;
import com.aisearch.service.Schemas;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

public class PDFExtractorTest {
    @Test
    public void testExtract() throws Exception {
        // Mock the JdbcRepository
        JdbcRepository mockJdbcRepository = mock(JdbcRepository.class);

        // Create an instance of PDFExtractor with the mocked repository
        PDFExtractor pdfExtractor = new PDFExtractor(mockJdbcRepository,Schemas.DOCS);

        // Provide a sample PDF file as input
        InputStream inputStream = Files.newInputStream(Paths.get("D:\\\\tmp\\\\aisearch\\\\陈勇实用验方选.pdf"));

        // Call the extract method
        String extractedContent = pdfExtractor.extract(inputStream);

        inputStream = Files.newInputStream(Paths.get("D:\\\\tmp\\\\aisearch\\\\陈勇实用验方选.pdf"));
        PDFTextStripper stripper = new PDFTextStripper();
        PDDocument document = Loader.loadPDF(inputStream.readAllBytes());
        String pageText = stripper.getText(document);
        inputStream.close();
        System.out.println("Extracted page text: " + pageText);

        // Assert that the extracted content is not null or empty
        assertNotNull(extractedContent);
        assertFalse(extractedContent.isEmpty());

    }
}
