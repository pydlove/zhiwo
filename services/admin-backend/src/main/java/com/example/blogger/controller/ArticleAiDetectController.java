// FIXME: AI 检测功能暂时禁用，准确度不足，待后续改进
// package com.example.blogger.controller;
//
// import com.example.blogger.entity.Result;
// import com.example.blogger.util.TextAiDetectUtil;
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.web.bind.annotation.*;
//
// import java.net.URI;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;
// import java.time.Duration;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
//
// @RestController
// @RequestMapping("/api/article-ai-detect")
// @CrossOrigin(origins = "*")
// public class ArticleAiDetectController {
//
//     private final ObjectMapper objectMapper = new ObjectMapper();
//
//     @Value("${ai.detect.enabled:true}")
//     private boolean aiDetectEnabled;
//
//     @Value("${ai.detect.url:http://127.0.0.1:5000}")
//     private String aiDetectUrl;
//
//     @Value("${ai.detect.timeout-ms:5000}")
//     private int aiDetectTimeoutMs;
//
//     private final HttpClient httpClient = HttpClient.newBuilder()
//             .connectTimeout(Duration.ofSeconds(3))
//             .build();
//
//     @PostMapping
//     public Result<Map<String, Object>> detect(@RequestBody Map<String, String> req) {
//         String content = req.get("content");
//         if (content == null || content.trim().length() < 10) {
//             return Result.error("文章内容不能少于10个字");
//         }
//
//         // 优先调用 Python 模型服务
//         if (aiDetectEnabled) {
//             Result<Map<String, Object>> modelResult = callPythonDetectService(content);
//             if (modelResult != null) {
//                 return modelResult;
//             }
//             // 调用失败则 fallback 到本地规则检测
//         }
//
//         // 本地规则检测（fallback）
//         return localDetect(content);
//     }
//
//     private Result<Map<String, Object>> callPythonDetectService(String content) {
//         try {
//             Map<String, String> body = new HashMap<>();
//             body.put("content", content);
//
//             HttpRequest request = HttpRequest.newBuilder()
//                     .uri(URI.create(aiDetectUrl + "/detect"))
//                     .header("Content-Type", "application/json")
//                     .timeout(Duration.ofMillis(aiDetectTimeoutMs))
//                     .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
//                     .build();
//
//             HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//
//             if (response.statusCode() == 200) {
//                 JsonNode node = objectMapper.readTree(response.body());
//
//                 Map<String, Object> data = new HashMap<>();
//                 data.put("score", node.get("score").asInt());
//                 data.put("level", node.get("level").asText());
//                 data.put("reasons", objectMapper.convertValue(node.get("reasons"), List.class));
//                 data.put("charCount", node.get("char_count").asInt());
//                 data.put("wordCount", node.get("word_count").asInt());
//                 data.put("model", node.get("model").asText());
//                 data.put("elapsedMs", node.get("elapsed_ms").asInt());
//                 data.put("source", "model");
//
//                 return Result.ok(data);
//             }
//         } catch (Exception e) {
//             System.err.println("AI 检测服务调用失败: " + e.getMessage());
//         }
//         return null;
//     }
//
//     private Result<Map<String, Object>> localDetect(String content) {
//         TextAiDetectUtil.AiDetectResult result = TextAiDetectUtil.detect(content);
//
//         Map<String, Object> data = new HashMap<>();
//         data.put("score", result.getScore());
//         data.put("level", result.getLevel());
//         data.put("reasons", result.getReasons());
//         data.put("charCount", content.length());
//         data.put("wordCount", content.trim().split("\\s+").length);
//         data.put("model", "rule-based");
//         data.put("source", "rule");
//
//         return Result.ok(data);
//     }
// }