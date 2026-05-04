package com.example.blogger.service;

import com.example.blogger.entity.ProcessAutoConfig;
import com.example.blogger.entity.ProcessDailyLog;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 线上服务代理：本地环境通过 HTTP 调用线上 admin-backend API 获取流程管理真实状态。
 * 避免本地直连生产数据库，降低安全风险。
 */
@Service
public class OnlineProxyService {

    private static final Logger log = LoggerFactory.getLogger(OnlineProxyService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${online.api.base-url:}")
    private String onlineBaseUrl;

    @Value("${online.api.secret:}")
    private String onlineSecret;

    /**
     * 是否启用线上代理模式
     */
    public boolean isEnabled() {
        return onlineBaseUrl != null && !onlineBaseUrl.trim().isEmpty();
    }

    /**
     * 调用线上 /api/process-auto/status
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getStatus(LocalDate date) {
        String url = buildUrl("/api/process-auto/status", date != null ? Map.of("date", date.toString()) : null);
        return getForObject(url, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * 调用线上 /api/process-auto/config
     */
    public ProcessAutoConfig getConfig() {
        String url = buildUrl("/api/process-auto/config", null);
        return getForObject(url, new TypeReference<ProcessAutoConfig>() {});
    }

    /**
     * 调用线上 /api/process-auto/today-log
     */
    public ProcessDailyLog getTodayLog(LocalDate date) {
        String url = buildUrl("/api/process-auto/today-log", date != null ? Map.of("date", date.toString()) : null);
        return getForObject(url, new TypeReference<ProcessDailyLog>() {});
    }

    /**
     * 调用线上 /api/process-auto/recent-logs
     */
    public List<ProcessDailyLog> getRecentLogs(int limit) {
        String url = buildUrl("/api/process-auto/recent-logs", Map.of("limit", String.valueOf(limit)));
        return getForObject(url, new TypeReference<List<ProcessDailyLog>>() {});
    }

    /**
     * 调用线上 /api/process-auto/trigger-check
     */
    public ProcessDailyLog triggerCheck(LocalDate date) {
        String url = buildUrl("/api/process-auto/trigger-check", date != null ? Map.of("date", date.toString()) : null);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addSecretHeader(headers);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        return parseResult(response.getBody(), new TypeReference<ProcessDailyLog>() {});
    }

    private String buildUrl(String path, Map<String, String> params) {
        StringBuilder url = new StringBuilder();
        // 去除末尾斜杠
        String base = onlineBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        url.append(base).append(path);
        if (params != null && !params.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) url.append("&");
                url.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
        }
        return url.toString();
    }

    private void addSecretHeader(HttpHeaders headers) {
        if (onlineSecret != null && !onlineSecret.isEmpty()) {
            headers.add("X-Internal-Secret", onlineSecret);
        }
    }

    private <T> T getForObject(String url, TypeReference<T> typeRef) {
        HttpHeaders headers = new HttpHeaders();
        addSecretHeader(headers);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return parseResult(response.getBody(), typeRef);
    }

    @SuppressWarnings("unchecked")
    private <T> T parseResult(String body, TypeReference<T> typeRef) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        try {
            Map<String, Object> result = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
            Integer code = (Integer) result.get("code");
            if (code == null || code != 200) {
                String msg = result.get("msg") != null ? result.get("msg").toString() : "请求失败";
                log.warn("[OnlineProxyService] 线上接口返回错误: {} - {}", code, msg);
                return null;
            }
            Object data = result.get("data");
            if (data == null) {
                return null;
            }
            // 如果是 Map 类型且目标类型也是 Map，直接返回
            if (typeRef.getType().getTypeName().contains("Map")) {
                return (T) data;
            }
            // 否则序列化后再反序列化
            String dataJson = objectMapper.writeValueAsString(data);
            return objectMapper.readValue(dataJson, typeRef);
        } catch (Exception e) {
            log.error("[OnlineProxyService] 解析响应失败: {}", e.getMessage());
            return null;
        }
    }
}
