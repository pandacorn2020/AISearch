package com.aisearch.util;

import com.aisearch.repository.JdbcRepository;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PDFImageProcessEngineTest {

    private PDFImageProcessEngine pdfImageProcessEngine;

    @BeforeEach
    void setUp() throws IOException {
        JdbcRepository jdbcRepository = mock(JdbcRepository.class);

        List<TextPosition> pageTextPositions = new ArrayList<>();
        pageTextPositions.add(createMockTextPosition(100, 100, 12, "Text1"));
        pageTextPositions.add(createMockTextPosition(110, 480, 12, "Text2"));
        pageTextPositions.add(createMockTextPosition(20, 500, 12, "Text3"));
        pageTextPositions.add(createMockTextPosition(110, 500, 12, "Text4"));
        pageTextPositions.add(createMockTextPosition(200, 480, 12, "Text5"));

        // Initialize PDFImageProcessEngine
        pdfImageProcessEngine = new PDFImageProcessEngine(
                1, // pageIndex
                1, // pageCount
                600, // pageHeight
                pageTextPositions,
                "Fallback text",
                jdbcRepository
        );
    }

    @Test
    void testExtractTextBelowImage() {
        // Act
        String extractedText = pdfImageProcessEngine.extractTextBelowImage(90, 470, 50);

        // Assert
        assertEquals("Text2Text5", extractedText);
    }

    private TextPosition createMockTextPosition(float x, float y, float fontSize, String unicode) {
        TextPosition mockTextPosition = mock(TextPosition.class);
        when(mockTextPosition.getX()).thenReturn(x);
        when(mockTextPosition.getY()).thenReturn(y);
        when(mockTextPosition.getFontSize()).thenReturn(fontSize);
        when(mockTextPosition.getUnicode()).thenReturn(unicode);
        return mockTextPosition;
    }
}