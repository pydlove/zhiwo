package com.example.blogger.controller;

import com.example.blogger.entity.ProcessAutoConfig;
import com.example.blogger.entity.ProcessDailyLog;
import com.example.blogger.entity.Result;
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

    public ProcessAutoController(ProcessAutoService processAutoService) {
        this.processAutoService = processAutoService;
    }

    @GetMapping("/config")
    public Result<ProcessAutoConfig> getConfig() {
        return Result.ok(processAutoService.getConfig());
    }

    @PostMapping("/config")
    public Result<Void> saveConfig(@RequestBody ProcessAutoConfig config) {
        processAutoService.saveConfig(config);
        return Result.ok(null);
    }

    @GetMapping("/today-log")
    public Result<ProcessDailyLog> getTodayLog(@RequestParam(value = "date", required = false) String dateStr) {
        LocalDate targetDate = null;
        if (dateStr != null && !dateStr.isEmpty()) {
            targetDate = LocalDate.parse(dateStr);
        }
        return Result.ok(processAutoService.getOrCreateLog(targetDate));
    }

    @GetMapping("/recent-logs")
    public Result<List<ProcessDailyLog>> getRecentLogs(@RequestParam(value = "limit", defaultValue = "7") int limit) {
        return Result.ok(processAutoService.getRecentLogs(limit));
    }

    @PostMapping("/trigger-check")
    public Result<ProcessDailyLog> triggerCheck(@RequestParam(value = "date", required = false) String dateStr) {
        LocalDate targetDate = null;
        if (dateStr != null && !dateStr.isEmpty()) {
            targetDate = LocalDate.parse(dateStr);
        }
        ProcessDailyLog log = processAutoService.triggerCheck(targetDate);
        return Result.ok(log);
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> getFullStatus(@RequestParam(value = "date", required = false) String dateStr) {
        Map<String, Object> result = new HashMap<>();
        LocalDate targetDate = null;
        if (dateStr != null && !dateStr.isEmpty()) {
            targetDate = LocalDate.parse(dateStr);
        }
        ProcessAutoConfig config = processAutoService.getConfig();
        ProcessDailyLog todayLog = processAutoService.getOrCreateLog(targetDate);
        List<ProcessDailyLog> recentLogs = processAutoService.getRecentLogs(7);
        result.put("config", config);
        result.put("todayLog", todayLog);
        result.put("recentLogs", recentLogs);
        return Result.ok(result);
    }
}
