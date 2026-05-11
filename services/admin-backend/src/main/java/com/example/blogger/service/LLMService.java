package com.example.blogger.service;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.blogger.mapper.ConfigMapper;
import com.example.blogger.entity.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LLMService {

    private static final String KIMI_API_URL = "https://api.kimi.com/coding";
    private static final String MINIMAX_API_URL = "https://api.minimax.chat/v1/chat/completions";

    private static final int CONNECT_TIMEOUT_MS = 30000;
    private static final int READ_TIMEOUT_MS = 300000;

    private final ConfigMapper configMapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public LLMService(ConfigMapper configMapper) {
        this.configMapper = configMapper;
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
     * 调用 Kimi K2.6 API (Spring AI OpenAI 兼容模式)
     */
    private String callKimiAPI(String prompt) {
        String apiKey = getConfigValue("apiKey");
        String model = getConfigValue("model");
        if (apiKey != null) apiKey = apiKey.trim();

        System.out.println("[LLMService] Kimi API Key length: " + (apiKey != null ? apiKey.length() : 0));
        System.out.println("[LLMService] Kimi model: " + model);

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("Kimi API Key 未配置，请在系统配置中设置");
        }
        if (model == null || model.isEmpty()) {
            model = "moonshot-v1-8k";
        }

        try {
            // 使用 Spring AI OpenAI 兼容模式调用 Kimi
            OpenAiApi openAiApi = new OpenAiApi(KIMI_API_URL, apiKey);
            OpenAiChatModel chatModel = new OpenAiChatModel(
                openAiApi,
                OpenAiChatOptions.builder()
                    .model(model)
                    .temperature(0.7)
                    .build()
            );

            return chatModel.call(prompt);
        } catch (Exception e) {
            System.err.println("[LLMService] Kimi API 调用异常: " + e.getClass().getName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("[LLMService] 根因: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            }
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

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        RestTemplate restTemplate = new RestTemplate(factory);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", new Object[]{
            Map.of("role", "user", "content", prompt)
        });

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(MINIMAX_API_URL, HttpMethod.POST, entity, String.class);
        } catch (HttpClientErrorException e) {
            System.err.println("[LLMService] MiniMax API 请求失败: " + e.getStatusCode());
            System.err.println("[LLMService] 响应体: " + e.getResponseBodyAsString());
            throw new RuntimeException("MiniMax API 认证失败 (" + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e);
        }

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
