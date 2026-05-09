package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.ScheduledPush;
import com.example.blogger.service.ScheduledPushService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduled-push")
@CrossOrigin(origins = "*")
public class ScheduledPushController {
    private final ScheduledPushService scheduledPushService;

    public ScheduledPushController(ScheduledPushService scheduledPushService) {
        this.scheduledPushService = scheduledPushService;
    }

    @PostMapping
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        String pushTime = body.get("pushTime") != null ? body.get("pushTime").toString() : null;
        String userFilterType = body.get("userFilterType") != null ? body.get("userFilterType").toString() : "all";
        String createdBy = body.get("createdBy") != null ? body.get("createdBy").toString() : null;
        @SuppressWarnings("unchecked")
        List<String> userIds = body.get("userIds") != null ? (List<String>) body.get("userIds") : null;
        if (pushTime == null || pushTime.isEmpty()) {
            return Result.error("推送时间不能为空");
        }
        scheduledPushService.create(pushTime, userFilterType, userIds, createdBy);
        return Result.ok(null);
    }

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return Result.ok(scheduledPushService.list(status, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<ScheduledPush> get(@PathVariable String id) {
        return Result.ok(scheduledPushService.getById(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> cancel(@PathVariable String id) {
        scheduledPushService.cancel(id);
        return Result.ok(null);
    }
}
