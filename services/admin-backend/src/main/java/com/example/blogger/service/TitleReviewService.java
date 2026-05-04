package com.example.blogger.service;

import com.example.blogger.entity.ServerConfig;
import com.example.blogger.entity.TitleLibrary;
import com.example.blogger.entity.TitlePushLog;
import com.example.blogger.entity.TitleReview;
import com.example.blogger.entity.Track;
import com.example.blogger.mapper.ServerConfigMapper;
import com.example.blogger.mapper.TitleLibraryMapper;
import com.example.blogger.mapper.TitlePushLogMapper;
import com.example.blogger.mapper.TitleReviewMapper;
import com.example.blogger.mapper.TrackMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class TitleReviewService {

    private static final Logger log = LoggerFactory.getLogger(TitleReviewService.class);

    private final TitleReviewMapper titleReviewMapper;
    private final TitleLibraryMapper titleLibraryMapper;
    private final TitlePushLogMapper titlePushLogMapper;
    private final ServerConfigMapper serverConfigMapper;
    private final TrackMapper trackMapper;
    private final RestTemplate restTemplate;

    // 轮询索引，用于多配置时的负载均衡
    private final java.util.concurrent.atomic.AtomicInteger roundRobinIndex = new java.util.concurrent.atomic.AtomicInteger(0);

    public TitleReviewService(TitleReviewMapper titleReviewMapper, TitleLibraryMapper titleLibraryMapper,
                              TitlePushLogMapper titlePushLogMapper, ServerConfigMapper serverConfigMapper,
                              TrackMapper trackMapper) {
        this.titleReviewMapper = titleReviewMapper;
        this.titleLibraryMapper = titleLibraryMapper;
        this.titlePushLogMapper = titlePushLogMapper;
        this.serverConfigMapper = serverConfigMapper;
        this.trackMapper = trackMapper;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 自动选择推送目标服务器配置
     * 1. 优先使用传入的 serverConfigId
     * 2. 其次查找标记为 default 的启用配置
     * 3. 如果都没有，从所有启用配置中轮询选择
     */
    private ServerConfig resolveServerConfig(String serverConfigId) {
        // 1. 如果传了具体配置ID，直接使用
        if (serverConfigId != null && !serverConfigId.isEmpty()) {
            ServerConfig config = serverConfigMapper.findById(serverConfigId);
            if (config != null && (config.getIsActive() == null || Integer.valueOf(1).equals(config.getIsActive()))) {
                log.info("[resolveServerConfig] 使用指定配置: id={}, name={}", config.getId(), config.getName());
                return config;
            }
            log.warn("[resolveServerConfig] 指定配置不存在或未启用: id={}", serverConfigId);
        }

        // 2. 查找默认配置
        ServerConfig defaultConfig = serverConfigMapper.findDefault();
        if (defaultConfig != null) {
            log.info("[resolveServerConfig] 使用默认配置: id={}, name={}", defaultConfig.getId(), defaultConfig.getName());
            return defaultConfig;
        }

        // 3. 从所有启用配置中轮询
        List<ServerConfig> activeConfigs = serverConfigMapper.findAllActive();
        if (activeConfigs != null && !activeConfigs.isEmpty()) {
            int idx = Math.abs(roundRobinIndex.getAndIncrement()) % activeConfigs.size();
            ServerConfig config = activeConfigs.get(idx);
            log.info("[resolveServerConfig] 轮询使用配置: idx={}, id={}, name={}", idx, config.getId(), config.getName());
            return config;
        }

        log.error("[resolveServerConfig] 没有可用的服务器配置");
        return null;
    }

    public Map<String, Object> listByStatus(String reviewStatus, String platform, String trackId, String keyword, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<TitleReview> list = titleReviewMapper.findByReviewStatus(reviewStatus, platform, trackId, keyword, offset, pageSize);
        int total = titleReviewMapper.countByReviewStatus(reviewStatus, platform, trackId, keyword);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    @Transactional
    public void approve(String id, String reviewedBy) {
        TitleReview review = titleReviewMapper.findById(id);
        if (review == null) {
            throw new RuntimeException("审核记录不存在");
        }
        if (!"pending".equals(review.getReviewStatus())) {
            throw new RuntimeException("该记录不在待审核状态");
        }
        review.setReviewStatus("approved");
        review.setReviewedBy(reviewedBy);
        review.setReviewedAt(LocalDateTime.now());
        titleReviewMapper.updateReviewStatus(review);
    }

    @Transactional
    public void reject(String id, String reason, String reviewedBy) {
        TitleReview review = titleReviewMapper.findById(id);
        if (review == null) {
            throw new RuntimeException("审核记录不存在");
        }
        // 允许拒绝：待审核 或 已通过但未推送 的记录
        if (!"pending".equals(review.getReviewStatus()) && !("approved".equals(review.getReviewStatus()) && "unpushed".equals(review.getPushStatus()))) {
            throw new RuntimeException("该记录无法拒绝：不在待审核状态或已推送");
        }
        review.setReviewStatus("rejected");
        review.setReviewReason(reason);
        review.setReviewedBy(reviewedBy);
        review.setReviewedAt(LocalDateTime.now());
        titleReviewMapper.updateReviewStatus(review);
    }

    @Transactional
    public void cancelReview(String id) {
        TitleReview review = titleReviewMapper.findById(id);
        if (review == null) {
            throw new RuntimeException("审核记录不存在");
        }
        if (!"approved".equals(review.getReviewStatus())) {
            throw new RuntimeException("该记录未通过审核，无法取消");
        }
        if (!"unpushed".equals(review.getPushStatus())) {
            throw new RuntimeException("该记录已推送，无法取消审核");
        }
        review.setReviewStatus("pending");
        review.setReviewReason(null);
        review.setReviewedBy(null);
        review.setReviewedAt(null);
        titleReviewMapper.updateReviewStatus(review);
    }

    @Transactional
    public void batchApprove(List<String> ids, String reviewedBy) {
        for (String id : ids) {
            try {
                approve(id, reviewedBy);
            } catch (Exception e) {
                // 跳过失败的
            }
        }
    }

    @Transactional
    public void batchReject(List<String> ids, String reason, String reviewedBy) {
        for (String id : ids) {
            try {
                reject(id, reason, reviewedBy);
            } catch (Exception e) {
                // 跳过失败的
            }
        }
    }

    @Transactional
    public void batchCancel(List<String> ids) {
        for (String id : ids) {
            try {
                cancelReview(id);
            } catch (Exception e) {
                // 跳过失败的
            }
        }
    }

    public Map<String, Object> listPushed(String platform, String trackId, String keyword, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<TitleReview> list = titleReviewMapper.findPushed(platform, trackId, keyword, offset, pageSize);
        int total = titleReviewMapper.countPushed(platform, trackId, keyword);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        // 使用和列表查询一致的过滤条件，确保数字和列表显示一致
        stats.put("pending", titleReviewMapper.countByReviewStatus("pending", null, null, null));
        stats.put("approved", titleReviewMapper.countByReviewStatus("approved", null, null, null));
        stats.put("rejected", titleReviewMapper.countByReviewStatus("rejected", null, null, null));
        stats.put("pushed", titleReviewMapper.countPushed(null, null, null));
        return stats;
    }

    @Transactional
    public Map<String, Object> pushToServer(String reviewId, String serverConfigId, String pushedBy) {
        Map<String, Object> result = new HashMap<>();
        TitleReview review = titleReviewMapper.findById(reviewId);
        if (review == null) {
            result.put("success", false);
            result.put("error", "审核记录不存在");
            return result;
        }
        if (!"approved".equals(review.getReviewStatus())) {
            result.put("success", false);
            result.put("error", "该记录未通过审核，无法推送");
            return result;
        }

        ServerConfig config = resolveServerConfig(serverConfigId);
        if (config == null) {
            result.put("success", false);
            result.put("error", "没有可用的服务器配置，请先添加并启用配置");
            return result;
        }

        TitleLibrary titleLibrary = titleLibraryMapper.findById(review.getTitleLibraryId());
        if (titleLibrary == null) {
            result.put("success", false);
            result.put("error", "标题库记录不存在");
            return result;
        }

        // 推送逻辑：通过 HTTP 调用目标服务接口
        String errorMsg = pushTitleToRemoteApi(config, titleLibrary);

        TitlePushLog pushLog = new TitlePushLog();
        pushLog.setId(UUID.randomUUID().toString().replace("-", ""));
        pushLog.setTitleLibraryId(titleLibrary.getId());
        pushLog.setServerConfigId(config.getId());
        pushLog.setTitle(titleLibrary.getTitle());
        pushLog.setPlatform(titleLibrary.getPlatform());
        pushLog.setTrackId(titleLibrary.getTrackId());
        pushLog.setPushedBy(pushedBy);

        if (errorMsg == null) {
            // 更新审核记录推送状态
            review.setPushStatus("pushed");
            review.setPushedAt(LocalDateTime.now());
            titleReviewMapper.updatePushStatus(review);

            pushLog.setStatus("success");
            titlePushLogMapper.insert(pushLog);

            result.put("success", true);
        } else {
            pushLog.setStatus("failed");
            pushLog.setErrorMsg(errorMsg);
            titlePushLogMapper.insert(pushLog);

            result.put("success", false);
            result.put("error", errorMsg);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> batchPushToServer(List<String> reviewIds, String serverConfigId, String pushedBy) {
        Map<String, Object> result = new HashMap<>();
        int success = 0;
        int failed = 0;
        List<Map<String, String>> errors = new ArrayList<>();

        for (String reviewId : reviewIds) {
            Map<String, Object> singleResult = pushToServer(reviewId, serverConfigId, pushedBy);
            if (Boolean.TRUE.equals(singleResult.get("success"))) {
                success++;
            } else {
                failed++;
                errors.add(Map.of("reviewId", reviewId, "error", (String) singleResult.get("error")));
            }
        }

        result.put("total", reviewIds.size());
        result.put("success", success);
        result.put("failed", failed);
        result.put("errors", errors);
        return result;
    }

    public Map<String, Object> listPushLogs(String status, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<TitlePushLog> list = titlePushLogMapper.findAll(status, offset, pageSize);
        int total = titlePushLogMapper.countAll(status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    /**
     * 测试目标服务连接（通过调用健康检查接口）
     */
    public boolean testConnection(ServerConfig config) {
        String baseUrl = getBaseUrl(config);
        String healthUrl = baseUrl + "/health";
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            // 如果没有健康检查接口，尝试调用标题库列表接口
            String fallbackUrl = baseUrl + "/api/title-library?page=1&pageSize=1";
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(fallbackUrl, Map.class);
                return response.getStatusCode().is2xxSuccessful();
            } catch (Exception ex) {
                return false;
            }
        }
    }

    /**
     * 推送标题到远程服务器（通过 HTTP API）
     */
    private String pushTitleToRemoteApi(ServerConfig config, TitleLibrary titleLibrary) {
        String baseUrl = getBaseUrl(config);
        String pushUrl = baseUrl + "/api/title-library/push-receive";

        String trackId = titleLibrary.getTrackId();
        String trackName = titleLibrary.getTrackName();
        if ((trackName == null || trackName.isEmpty()) && trackId != null && !trackId.isEmpty()) {
            Track track = trackMapper.findById(trackId);
            if (track != null) {
                trackName = track.getName();
            }
        }
        log.info("[push] title={}, trackId={}, trackName(fromTitleLibrary)={}, trackName(fallback)={}",
                titleLibrary.getTitle(), trackId, titleLibrary.getTrackName(), trackName);

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", titleLibrary.getId());
        payload.put("title", titleLibrary.getTitle());
        payload.put("description", titleLibrary.getDescription());
        payload.put("platform", titleLibrary.getPlatform());
        payload.put("trackId", trackId);
        payload.put("trackName", trackName);
        payload.put("pushDate", titleLibrary.getPushDate() != null ? titleLibrary.getPushDate().toString() : null);
        payload.put("useCount", titleLibrary.getUseCount());
        payload.put("isUsed", titleLibrary.getIsUsed());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(pushUrl, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                // 目标服务返回格式: { "code": 200, "data": {...}, "msg": "..." }
                Object code = body.get("code");
                if (code != null) {
                    int codeInt = code instanceof Number ? ((Number) code).intValue() : Integer.parseInt(code.toString());
                    if (codeInt == 200 || codeInt == 0) {
                        return null; // 成功
                    }
                }
                return (String) body.getOrDefault("msg", "目标服务返回错误");
            }
            return "目标服务响应异常：HTTP " + response.getStatusCode();
        } catch (Exception e) {
            return "调用目标服务失败：" + e.getMessage();
        }
    }

    /**
     * 获取目标服务的基础地址
     */
    private String getBaseUrl(ServerConfig config) {
        return "http://" + config.getHost() + ":" + config.getPort();
    }

    @Transactional
    public TitleReview createReviewRecord(String titleLibraryId, String source) {
        TitleReview review = new TitleReview();
        review.setId(UUID.randomUUID().toString().replace("-", ""));
        review.setTitleLibraryId(titleLibraryId);
        review.setReviewStatus("pending");
        review.setPushStatus("unpushed");
        review.setSource(source != null ? source : "ai_generated");
        titleReviewMapper.insert(review);
        return review;
    }
}
