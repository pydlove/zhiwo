package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.TitleGenerateTask;
import com.example.blogger.service.TitleGenerateTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/title-generate")
public class TitleGenerateController {

    private static final Logger log = LoggerFactory.getLogger(TitleGenerateController.class);

    private final TitleGenerateTaskService taskService;
    private final ObjectMapper objectMapper;

    public TitleGenerateController(TitleGenerateTaskService taskService, ObjectMapper objectMapper) {
        this.taskService = taskService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/tasks")
    public Result<Map<String, Object>> createTask(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> platforms = (List<String>) body.get("platforms");
        @SuppressWarnings("unchecked")
        List<String> trackIds = (List<String>) body.get("trackIds");
        Integer countPerCombo = body.get("countPerCombo") != null ? (Integer) body.get("countPerCombo") : 3;
        String instruction = body.get("instruction") != null ? (String) body.get("instruction") : "";

        try {
            String platformsJson = platforms != null ? objectMapper.writeValueAsString(platforms) : null;
            String trackIdsJson = trackIds != null ? objectMapper.writeValueAsString(trackIds) : null;
            TitleGenerateTask task = taskService.createTask(platformsJson, trackIdsJson, countPerCombo, instruction);
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", task.getId());
            result.put("status", "pending");
            result.put("message", "生成任务已创建，系统将在后台自动处理");
            log.info("[TitleGenerateController] 任务已创建: id={}", task.getId());
            return Result.ok(result);
        } catch (Exception e) {
            log.error("[TitleGenerateController] 创建任务失败: {}", e.getMessage(), e);
            return Result.error("创建任务失败: " + e.getMessage());
        }
    }

    @GetMapping("/tasks")
    public Result<Map<String, Object>> listTasks(@RequestParam(required = false) String status) {
        List<TitleGenerateTask> list = taskService.listTasks(status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", list.size());
        return Result.ok(result);
    }

    @GetMapping("/tasks/{id}")
    public Result<TitleGenerateTask> getTask(@PathVariable String id) {
        TitleGenerateTask task = taskService.findById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        return Result.ok(task);
    }

    @PostMapping("/tasks/{id}/cancel")
    public Result<Void> cancelTask(@PathVariable String id) {
        TitleGenerateTask task = taskService.findById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        boolean success = taskService.cancelTask(id);
        if (!success) {
            return Result.error("只能取消排队中的任务");
        }
        log.info("[TitleGenerateController] 任务已取消: id={}", id);
        return Result.ok(null);
    }
}
