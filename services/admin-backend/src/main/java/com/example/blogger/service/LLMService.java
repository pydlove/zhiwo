package com.example.blogger.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.blogger.mapper.ConfigMapper;
import com.example.blogger.entity.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
public class LLMService {

    @Autowired
    private ConfigMapper configMapper;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final int TIMEOUT_MS = 300000; // 5分钟

    public LLMService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取当前配置的默认模型类型
     */
    public String getSelectedModelType() {
        String val = getConfigValue("selectedLLMModel");
        return (val == null || val.isEmpty()) ? "kimi" : val;
    }

    /**
     * 调用大模型生成内容
     */
    public String generateContent(String prompt) {
        String modelType = getSelectedModelType();
        if ("minimax".equals(modelType)) {
            return callMinimaxAPI(prompt);
        } else {
            return callKimiAPI(prompt);
        }
    }

    /**
     * 调用 Kimi K2.6 API
     */
    private String callKimiAPI(String prompt) {
        String apiKey = getConfigValue("apiKey");
        String model = getConfigValue("model");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("Kimi API Key 未配置");
        }
        if (model == null || model.isEmpty()) {
            model = "moonshot-v1-8k";
        }

        String url = "https://api.moonshot.cn/v1/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", new Object[]{
            Map.of("role", "user", "content", prompt)
        });

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                return choices.get(0).get("message").get("content").asText();
            }
            throw new RuntimeException("Kimi API 返回格式异常: " + response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Kimi API 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用 MiniMax M2.7 API
     */
    private String callMinimaxAPI(String prompt) {
        String apiKey = getConfigValue("miniMaxApiKey");
        String model = getConfigValue("miniMaxModel");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("MiniMax API Key 未配置");
        }
        if (model == null || model.isEmpty()) {
            model = "MiniMax-M2.7";
        }

        String url = "https://api.minimax.chat/v1/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", new Object[]{
            Map.of("role", "user", "content", prompt)
        });

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                return choices.get(0).get("message").get("content").asText();
            }
            throw new RuntimeException("MiniMax API 返回格式异常: " + response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("MiniMax API 调用失败: " + e.getMessage(), e);
        }
    }

    private String getConfigValue(String key) {
        List<Config> configs = configMapper.findAll();
        for (Config c : configs) {
            if (key.equals(c.getConfigKey())) {
                return c.getConfigValue();
            }
        }
        return null;
    }
}