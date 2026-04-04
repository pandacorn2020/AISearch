package com.aisearch.util;

import com.aisearch.service.DocumentLoader;
import com.aisearch.service.FileService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

public class FileConverterTest {

    @Test
    public void testConvertPdfToText() {
        try {
            InputStream is = DocumentLoader.getInputStream("123.pptx");
            FileService fileService = new FileService();
            String text = fileService.convertFileToText(is, "test.pptx","unknown");
            System.out.println(text);
            Assertions.assertTrue(text.contains("一季度中国GDP"));
            Assertions.assertTrue(text.contains("国家药品监督管理局发布"));

        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("Failed to convert PDF to text");
        }
    }

    @Test
    public void testConvertWordToText() {
        try {
            InputStream is = DocumentLoader.getInputStream("100一个散结方.docx");
            String text = FileConverter.convertWordToText(is, "100一个散结方.docx");
            System.out.println(text);
            //Assertions.assertTrue(text.contains("一季度中国GDP"));
            //Assertions.assertTrue(text.contains("国家药品监督管理局发布"));

        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("Failed to convert Word to text");
        }
    }
}
