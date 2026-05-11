package com.example.blogger.util;

import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Kimi API 测试工具
 * 用法: java com.example.blogger.util.KimiApiTest <baseUrl> <apiKey> [model]
 * 示例: java com.example.blogger.util.KimiApiTest https://api.kimi.com/coding sk-xxxxxx moonshot-v1-8k
 */
public class KimiApiTest {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("用法: java com.example.blogger.util.KimiApiTest <baseUrl> <apiKey> [model]");
            System.out.println("示例: java com.example.blogger.util.KimiApiTest https://api.kimi.com/coding sk-xxxxxx moonshot-v1-8k");
            System.exit(1);
        }

        String baseUrl = args[0];
        String apiKey = args[1];
        String model = args.length > 2 ? args[2] : "moonshot-v1-8k";

        System.out.println("=== Kimi API 测试 ===");
        System.out.println("Base URL: " + baseUrl);
        System.out.println("API Key 长度: " + apiKey.length());
        System.out.println("API Key 前缀: " + apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
        System.out.println("Model: " + model);
        System.out.println();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(300000);
        RestTemplate restTemplate = new RestTemplate(factory);

        String url = baseUrl + "/v1/chat/completions";
        System.out.println("请求 URL: " + url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", new Object[]{
            Map.of("role", "user", "content", "你好，请用一句话介绍自己")
        });

        System.out.println("请求头: " + headers);
        System.out.println("请求体: " + body);
        System.out.println();

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            System.out.println("状态码: " + response.getStatusCode());
            System.out.println("响应体: " + response.getBody());
        } catch (Exception e) {
            System.err.println("请求失败: " + e.getClass().getName() + ": " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                System.err.println("\n❌ 401 认证失败 - API Key 无效或已过期");
            }
        }
    }
}
