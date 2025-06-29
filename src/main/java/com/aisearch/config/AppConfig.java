package com.aisearch.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration  // 声明该类为配置类
public class AppConfig {

    // 定义 ExecutorService bean
    @Bean
    public ExecutorService executorService() {
        // 创建一个固定大小的线程池
        return Executors.newFixedThreadPool(10);
    }

}