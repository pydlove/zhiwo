package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.TitleReview;
import com.example.blogger.service.TitleReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/title-review")
@CrossOrigin(origins = "*")
public class TitleReviewController {

    private final TitleReviewService titleReviewService;

    public TitleReviewController(TitleReviewService titleReviewService) {
        this.titleReviewService = titleReviewService;
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam("reviewStatus") String reviewStatus,
            @RequestParam(value = "platform", required = false) String platform,
            @RequestParam(value = "trackId", required = false) String trackId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize) {
        return Result.ok(titleReviewService.listByStatus(reviewStatus, platform, trackId, keyword, page, pageSize));
    }

    @GetMapping("/list-pushed")
    public Result<Map<String, Object>> listPushed(
            @RequestParam(value = "platform", required = false) String platform,
            @RequestParam(value = "trackId", required = false) String trackId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize) {
        return Result.ok(titleReviewService.listPushed(platform, trackId, keyword, page, pageSize));
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable String id) {
        String reviewedBy = getCurrentUserId();
        titleReviewService.approve(id, reviewedBy);
        return Result.ok(null);
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String reason = body != null ? (String) body.get("reason") : null;
        String reviewedBy = getCurrentUserId();
        titleReviewService.reject(id, reason, reviewedBy);
        return Result.ok(null);
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable String id) {
        titleReviewService.cancelReview(id);
        return Result.ok(null);
    }

    @PostMapping("/batch-approve")
    public Result<Void> batchApprove(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        String reviewedBy = getCurrentUserId();
        titleReviewService.batchApprove(ids, reviewedBy);
        return Result.ok(null);
    }

    @PostMapping("/batch-reject")
    public Result<Void> batchReject(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        String reason = (String) body.get("reason");
        String reviewedBy = getCurrentUserId();
        titleReviewService.batchReject(ids, reason, reviewedBy);
        return Result.ok(null);
    }

    @PostMapping("/batch-cancel")
    public Result<Void> batchCancel(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        titleReviewService.batchCancel(ids);
        return Result.ok(null);
    }

    @PostMapping("/{id}/push")
    public Result<Map<String, Object>> push(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String serverConfigId = (String) body.get("serverConfigId");
        String pushedBy = getCurrentUserId();
        Map<String, Object> result = titleReviewService.pushToServer(id, serverConfigId, pushedBy);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.ok(result);
        }
        return Result.error((String) result.get("error"));
    }

    @PostMapping("/batch-push")
    public Result<Map<String, Object>> batchPush(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        String serverConfigId = (String) body.get("serverConfigId");
        String pushedBy = getCurrentUserId();
        Map<String, Object> result = titleReviewService.batchPushToServer(ids, serverConfigId, pushedBy);
        return Result.ok(result);
    }

    @PostMapping("/{id}/re-push")
    public Result<Map<String, Object>> rePush(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String serverConfigId = body != null ? (String) body.get("serverConfigId") : null;
        String pushedBy = getCurrentUserId();
        Map<String, Object> result = titleReviewService.rePushToServer(id, serverConfigId, pushedBy);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.ok(result);
        }
        return Result.error((String) result.get("error"));
    }

    @PostMapping("/batch-re-push")
    public Result<Map<String, Object>> batchRePush(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.get("ids");
        String serverConfigId = (String) body.get("serverConfigId");
        String pushedBy = getCurrentUserId();
        Map<String, Object> result = new HashMap<>();
        int success = 0;
        int failed = 0;
        List<Map<String, String>> errors = new ArrayList<>();
        for (String id : ids) {
            Map<String, Object> singleResult = titleReviewService.rePushToServer(id, serverConfigId, pushedBy);
            if (Boolean.TRUE.equals(singleResult.get("success"))) {
                success++;
            } else {
                failed++;
                errors.add(Map.of("reviewId", id, "error", (String) singleResult.get("error")));
            }
        }
        result.put("total", ids.size());
        result.put("success", success);
        result.put("failed", failed);
        result.put("errors", errors);
        return Result.ok(result);
    }

    @GetMapping("/list-by-source")
    public Result<Map<String, Object>> listBySource(
            @RequestParam("source") String source,
            @RequestParam(value = "platform", required = false) String platform,
            @RequestParam(value = "trackId", required = false) String trackId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize) {
        return Result.ok(titleReviewService.listBySource(source, platform, trackId, keyword, page, pageSize));
    }

    @GetMapping("/push-logs")
    public Result<Map<String, Object>> pushLogs(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize) {
        return Result.ok(titleReviewService.listPushLogs(status, page, pageSize));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(titleReviewService.getStats());
    }

    private String getCurrentUserId() {
        // TODO: 从认证上下文获取当前用户ID，临时返回空字符串
        return "";
    }

    /**
     * AI 优化标题：对指定审核中的标题进行 Claude 优化
     */
    @PostMapping("/{id}/optimize")
    public Result<Map<String, Object>> optimizeTitle(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        String currentTitle = body != null ? (String) body.get("currentTitle") : null;
        String instruction = body != null ? (String) body.get("instruction") : null;
        if (currentTitle == null || currentTitle.isBlank()) {
            return Result.error("标题内容不能为空");
        }

        TitleReview review = titleReviewService.getById(id);
        if (review == null) {
            return Result.error("审核记录不存在");
        }
        if (!"pending".equals(review.getReviewStatus())) {
            return Result.error("只有待审核状态才能优化标题");
        }

        String trackName = review.getTrackName() != null ? review.getTrackName() : "";
        String platform = review.getPlatform() != null ? review.getPlatform() : "";

        String prompt = String.format("""
            请优化以下标题，使其更具吸引力、更符合%s平台%s赛道的风格。

            原始标题：%s
            """, platform, trackName, currentTitle);

        if (instruction != null && !instruction.isBlank()) {
            prompt += String.format("""
            优化方向要求：%s
            """, instruction);
        }

        prompt += """
            要求：
            1. 保持原意，适当改写，增强吸引力
            2. 不超过30个字符
            3. 不要加"深度"、"详解"、"全面"等空洞词汇
            4. 避免重复的感叹词和形容词

            输出格式（只输出优化后的标题，不要任何解释）：
            优化标题：[你的优化结果]
            """;

        String optimizedTitle;
        try {
            optimizedTitle = titleReviewService.optimizeTitleByClaude(prompt);
        } catch (Exception e) {
            return Result.error("AI 优化失败: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("originalTitle", currentTitle);
        result.put("optimizedTitle", optimizedTitle);
        result.put("id", id);
        return Result.ok(result);
    }
}
