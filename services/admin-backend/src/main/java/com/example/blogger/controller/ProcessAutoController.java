package com.example.blogger.controller;

import com.example.blogger.entity.ProcessAutoConfig;
import com.example.blogger.entity.ProcessDailyLog;
import com.example.blogger.entity.Result;
import com.example.blogger.service.OnlineProxyService;
import com.example.blogger.service.ProcessAutoService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/process-auto")
@CrossOrigin(origins = "*")
public class ProcessAutoController {

    private final ProcessAutoService processAutoService;
    private final OnlineProxyService onlineProxyService;

    public ProcessAutoController(ProcessAutoService processAutoService,
                                 OnlineProxyService onlineProxyService) {
        this.processAutoService = processAutoService;
        this.onlineProxyService = onlineProxyService;
    }

    @GetMapping("/config")
    public Result<ProcessAutoConfig> getConfig() {
        if (onlineProxyService.isEnabled()) {
            return Result.ok(onlineProxyService.getConfig());
        }
        return Result.ok(processAutoService.getConfig());
    }

    @PostMapping("/config")
    public Result<Void> saveConfig(@RequestBody ProcessAutoConfig config) {
        if (onlineProxyService.isEnabled()) {
            // 线上配置暂不支持通过本地修改，避免误操作
            return Result.error(403, "线上配置请登录线上后台修改");
        }
        processAutoService.saveConfig(config);
        return Result.ok(null);
    }

    @GetMapping("/today-log")
    public Result<ProcessDailyLog> getTodayLog(@RequestParam(value = "date", required = false) String dateStr) {
        LocalDate targetDate = null;
        if (dateStr != null && !dateStr.isEmpty()) {
            targetDate = LocalDate.parse(dateStr);
        }
        if (onlineProxyService.isEnabled()) {
            return Result.ok(onlineProxyService.getTodayLog(targetDate));
        }
        return Result.ok(processAutoService.getOrCreateLog(targetDate));
    }

    @GetMapping("/recent-logs")
    public Result<List<ProcessDailyLog>> getRecentLogs(@RequestParam(value = "limit", defaultValue = "7") int limit) {
        if (onlineProxyService.isEnabled()) {
            return Result.ok(onlineProxyService.getRecentLogs(limit));
        }
        return Result.ok(processAutoService.getRecentLogs(limit));
    }

    @PostMapping("/trigger-check")
    public Result<ProcessDailyLog> triggerCheck(@RequestParam(value = "date", required = false) String dateStr) {
        LocalDate targetDate = null;
        if (dateStr != null && !dateStr.isEmpty()) {
            targetDate = LocalDate.parse(dateStr);
        }
        if (onlineProxyService.isEnabled()) {
            return Result.ok(onlineProxyService.triggerCheck(targetDate));
        }
        ProcessDailyLog log = processAutoService.triggerCheck(targetDate);
        return Result.ok(log);
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> getFullStatus(@RequestParam(value = "date", required = false) String dateStr) {
        LocalDate targetDate = null;
        if (dateStr != null && !dateStr.isEmpty()) {
            targetDate = LocalDate.parse(dateStr);
        }
        if (onlineProxyService.isEnabled()) {
            return Result.ok(onlineProxyService.getStatus(targetDate));
        }
        Map<String, Object> result = new HashMap<>();
        ProcessAutoConfig config = processAutoService.getConfig();
        ProcessDailyLog todayLog = processAutoService.getOrCreateLog(targetDate);
        List<ProcessDailyLog> recentLogs = processAutoService.getRecentLogs(7);
        result.put("config", config);
        result.put("todayLog", todayLog);
        result.put("recentLogs", recentLogs);
        return Result.ok(result);
    }
}
