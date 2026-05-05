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
}
