package com.example.blogger.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.blogger.entity.LLMConfig;
import com.example.blogger.mapper.LLMConfigMapper;

import java.util.HashMap;
import java.util.Map;

@Service
public class LLMService {

    private static final Logger log = LoggerFactory.getLogger(LLMService.class);

    private static final String KIMI_BASE_URL = "https://api.kimi.com/coding";
    private static final String MINIMAX_API_URL = "https://api.minimax.chat/v1/chat/completions";

    private static final int CONNECT_TIMEOUT_MS = 30000;
    private static final int READ_TIMEOUT_MS = 300000;

    private final LLMConfigMapper llmConfigMapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public LLMService(LLMConfigMapper llmConfigMapper) {
        this.llmConfigMapper = llmConfigMapper;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取当前配置的默认模型类型
     */
    public String getSelectedModelType() {
        LLMConfig active = llmConfigMapper.findActive();
        String provider = (active != null && active.getProvider() != null) ? active.getProvider() : "kimi";
        log.info("[LLMService] 读取模型配置 activeProvider={}, 解析结果={}", provider, provider);
        return provider;
    }

    /**
     * 调用大模型生成内容
     */
    public String generateContent(String prompt) {
        String modelType = getSelectedModelType();
        log.info("[LLMService] 生成内容路由决策: modelType={}, 将使用{}", modelType, "minimax".equals(modelType) ? "MiniMax" : "Kimi");
        if ("minimax".equals(modelType)) {
            return callMinimaxAPI(prompt);
        } else {
            return callKimiAPI(prompt);
        }
    }

    /**
     * 调用 Kimi K2.6 API (java.net.http.HttpClient 精确复刻 CLI 请求)
     */
    private String callKimiAPI(String prompt) {
        LLMConfig config = llmConfigMapper.findByProvider("kimi");
        String apiKey = config != null ? config.getApiKey() : null;
        String model = config != null ? config.getModel() : null;
        if (apiKey != null) apiKey = apiKey.trim();

        log.info("[LLMService] Kimi API Key length: {}", (apiKey != null ? apiKey.length() : 0));
        log.info("[LLMService] Kimi model: {}", model);

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("Kimi API Key 未配置，请在系统配置中设置");
        }
        if (model == null || model.isEmpty()) {
            model = "moonshot-v1-8k";
        }

        try {
            // Build request body as raw JSON string
            String escapedPrompt;
            try {
                escapedPrompt = objectMapper.writeValueAsString(prompt);
            } catch (Exception e) {
                escapedPrompt = "\"" + prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
            }
            String bodyJson = "{\"model\":\"" + model + "\",\"messages\":[{\"role\":\"user\",\"content\":" + escapedPrompt + "}],\"temperature\":0.7,\"stream\":false,\"max_tokens\":8192}";

            log.info("[LLMService] Kimi request URL: {}", KIMI_BASE_URL + "/v1/chat/completions");
            log.info("[LLMService] Kimi request body: {}", bodyJson);

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(KIMI_BASE_URL + "/v1/chat/completions"))
                    .timeout(java.time.Duration.ofMillis(READ_TIMEOUT_MS))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("User-Agent", "claude-cli/2.1.110 (external, cli)")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(bodyJson, java.nio.charset.StandardCharsets.UTF_8))
                    .build();

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofMillis(CONNECT_TIMEOUT_MS))
                    .version(java.net.http.HttpClient.Version.HTTP_1_1)
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("[LLMService] Kimi API 请求失败: {}", response.statusCode());
                log.error("[LLMService] 响应体: {}", response.body());
                throw new RuntimeException("Kimi API 认证失败 (" + response.statusCode() + "): " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                return choices.get(0).get("message").get("content").asText();
            }
            throw new RuntimeException("Kimi API 返回格式异常: " + response.body());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Kimi API 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用 MiniMax M2.7 API (java.net.http.HttpClient 同步模式)
     */
    private String callMinimaxAPI(String prompt) {
        LLMConfig config = llmConfigMapper.findByProvider("minimax");
        String apiKey = config != null ? config.getApiKey() : null;
        String model = config != null ? config.getModel() : null;
        log.info("[LLMService] MiniMax API Key length: {}", (apiKey != null ? apiKey.length() : 0));
        log.info("[LLMService] MiniMax model: {}", model);
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("MiniMax API Key 未配置");
        }
        if (model == null || model.isEmpty()) {
            model = "MiniMax-M2.7";
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", new Object[]{
                Map.of("role", "user", "content", prompt)
            });
            String bodyJson = objectMapper.writeValueAsString(body);

            log.info("[LLMService] MiniMax request URL: {}", MINIMAX_API_URL);
            log.info("[LLMService] MiniMax request body: {}", bodyJson);

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(MINIMAX_API_URL))
                    .timeout(java.time.Duration.ofMillis(READ_TIMEOUT_MS))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(bodyJson, java.nio.charset.StandardCharsets.UTF_8))
                    .build();

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofMillis(CONNECT_TIMEOUT_MS))
                    .version(java.net.http.HttpClient.Version.HTTP_1_1)
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("[LLMService] MiniMax API 请求失败: {}", response.statusCode());
                log.error("[LLMService] 响应体: {}", response.body());
                throw new RuntimeException("MiniMax API 请求失败 (" + response.statusCode() + "): " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                return choices.get(0).get("message").get("content").asText();
            }
            throw new RuntimeException("MiniMax API 返回格式异常: " + response.body());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("MiniMax API 调用失败: " + e.getMessage(), e);
        }
    }
}
