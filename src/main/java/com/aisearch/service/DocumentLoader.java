package com.aisearch.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DocumentLoader {
    public static final int SPLIT_SIZE = 2000;
    public static final int OVERLAP_SIZE = SPLIT_SIZE / 5;

    public String[] splitText(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + SPLIT_SIZE);
            String chunk = text.substring(start, end);
            if (chunk.trim().isEmpty()) {
                start = end - OVERLAP_SIZE;
                continue;
            }
            chunks.add(chunk);
            if (start + chunk.length() >= text.length()) {
                break;
            }
            start = end - OVERLAP_SIZE;
        }
        return chunks.toArray(new String[0]);
    }


    public static InputStream getInputStream(String fileName) throws Exception {
        ClassLoader classLoader = DocumentLoader.class.getClassLoader();
        return classLoader.getResourceAsStream(fileName);
    }

    public List<String> readAllLines(String fileName) throws Exception {
        InputStream is = getInputStream(fileName);
        InputStreamReader reader = new InputStreamReader(is, "UTF-8");
        List<String> lines = new ArrayList<>();
        try {
            try (BufferedReader br = new BufferedReader(reader)) {
                String line = br.readLine();
                while (line != null) {
                    lines.add(line);
                    line = br.readLine();
                }
            }
        } finally {
            is.close();
        }
        return lines;
    }

    Map<String, String> promptMap = new ConcurrentHashMap<>();

    public String readPrompt(String promptFileName) {
        if (promptMap.containsKey(promptFileName)) {
            return promptMap.get(promptFileName);
        }
        try {
            InputStream is = getInputStream(promptFileName);
            try (InputStreamReader reader = new InputStreamReader(is, "UTF-8")) {
                StringJoiner builder = new StringJoiner("\n");
                try (BufferedReader br = new BufferedReader(reader)) {
                    String line = br.readLine();
                    while (line != null) {
                        builder.add(line);
                        line = br.readLine();
                    }
                }
                String prompt = builder.toString();
                promptMap.put(promptFileName, prompt);
                return prompt;
            } finally {
                is.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String readKgSystemPrompt() {
        return readPrompt("general_kg_system_prompt_zh.txt");
    }

    public String readSystemPrompt() {
        return readPrompt("general_system_prompt_zh.txt");
    }

    public String readKgUserPrompt() {
        return readPrompt("general_kg_user_prompt_zh.txt");
    }

    public String readKgCommunityPrompt() {
        return readPrompt("general_kg_community_prompt_zh.txt");
    }

    public String readKgDupRemovalPrompt()  {
        return readPrompt("general_kg_dup_removal_prompt_zh.txt");
    }


}


