package com.aisearch.util;

import com.aliyun.docmind_api20220711.Client;
import com.aliyun.docmind_api20220711.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.aisearch.config.KgProperties;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * 临时工具：打印 SDK 响应类的所有 getter 方法，用于探索 API 返回结构。
 */
public class SdkInspector {
    public static void main(String[] args) throws Exception {
        Properties appProps = new Properties();
        try (InputStream is = new FileInputStream("src/main/resources/application.properties")) {
            appProps.load(is);
        }

        KgProperties props = new KgProperties();
        props.setAliDocAccessKey(appProps.getProperty("kg.aliDocAccessKey", ""));
        props.setAliDocSecretKey(appProps.getProperty("kg.aliDocSecretKey", ""));

        // 打印关键类的所有方法
        System.out.println("=== QueryDocParserStatusResponseBody ===");
        printGetters(QueryDocParserStatusResponseBody.class);

        System.out.println("\n=== GetDocParserResultResponseBody ===");
        printGetters(GetDocParserResultResponseBody.class);

        System.out.println("\n=== SubmitDocParserJobResponseBody ===");
        printGetters(SubmitDocParserJobResponseBody.class);

        // 也检查嵌套的 Data 类
        try {
            Class<?> dataClass = QueryDocParserStatusResponseBody.class.getMethod("getData").getReturnType();
            System.out.println("\n=== QueryDocParserStatusResponseBody.getData() return type: " + dataClass.getName() + " ===");
            printGetters(dataClass);
        } catch (Exception e) {
            System.out.println("No getData: " + e.getMessage());
        }

        try {
            Class<?> dataClass = SubmitDocParserJobResponseBody.class.getMethod("getData").getReturnType();
            System.out.println("\n=== SubmitDocParserJobResponseBody.getData() return type: " + dataClass.getName() + " ===");
            printGetters(dataClass);
        } catch (Exception e) {
            System.out.println("No getData: " + e.getMessage());
        }
    }

    private static void printGetters(Class<?> clazz) {
        for (Method m : clazz.getMethods()) {
            if (m.getName().startsWith("get") && m.getParameterCount() == 0) {
                System.out.println("  " + m.getReturnType().getSimpleName() + " " + m.getName() + "()");
            }
        }
    }
}
