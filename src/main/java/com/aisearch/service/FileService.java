package com.aisearch.service;

import com.aisearch.util.FileConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;

@Service
public class FileService {
    @Autowired
    private PDFService pdfService;


    public String convertFileToText(InputStream inputStream, String description, String schema) {

        try {
            if (description.endsWith(".pdf")) {
                return pdfService.process(inputStream, schema);
            } else {
                return FileConverter.convertFileToText(inputStream, description);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error processing PDF file: " + e.getMessage(), e);
        }
    }
}
