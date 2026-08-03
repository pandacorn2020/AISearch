package com.aisearch.util;

import com.aliyun.docmind_api20220711.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.docmind_api20220711.Client;
import com.aliyun.teautil.models.RuntimeOptions;
import com.alibaba.fastjson.JSON;
import java.io.*;
import java.util.Properties;

public class AlliyunDoc {
    public static void main(String[] args) throws Exception {
        // 从 application.properties 读取 AccessKey
        Properties props = new Properties();
        try (InputStream is = new FileInputStream("src/main/resources/application.properties")) {
            props.load(is);
        }
        String accessKey = props.getProperty("kg.aliDocAccessKey", "");
        String secretKey = props.getProperty("kg.aliDocSecretKey", "");

        Config config = new Config()
            .setAccessKeyId(accessKey)
            .setAccessKeySecret(secretKey);
        // 访问的域名，支持IPv4和IPv6两种方式，IPv6请使用docmind-api-dualstack.cn-hangzhou.aliyuncs.com。
        config.endpoint = "docmind-api.cn-hangzhou.aliyuncs.com";
        Client client = new Client(config);
        RuntimeOptions runtime = new RuntimeOptions();
        SubmitDocStructureJobAdvanceRequest advanceRequest = new SubmitDocStructureJobAdvanceRequest();
        // 替换为实际的本地文件路径。
        File file = new File("D:/tmp/aisearch/陈勇实用验方选.pdf");
        advanceRequest.fileUrlObject = new FileInputStream(file);
        advanceRequest.fileName = "陈勇实用验方选.pdf";
        SubmitDocStructureJobResponse response = client.submitDocStructureJobAdvance(advanceRequest, runtime);
        Object obj = JSON.toJSON(response.getBody());
        System.out.println(obj.toString());
    }
}
