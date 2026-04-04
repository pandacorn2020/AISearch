package com.aisearch.service;

import com.aisearch.repository.JdbcRepository;
import com.aisearch.util.PDFExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class PDFService {

    @Autowired
    private JdbcRepository jdbcRepository;

    public String process(InputStream inputStream, String schema) {
        PDFExtractor extractor = new PDFExtractor(jdbcRepository, schema);
        try {
            return extractor.extract(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
