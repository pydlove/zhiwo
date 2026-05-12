package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.TitleGenerationTask;
import com.example.blogger.service.TitleGenerationTaskService;
import com.example.blogger.service.TitleLibraryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final TitleGenerationTaskService taskService;
    private final TitleLibraryService titleLibraryService;

    public TaskController(TitleGenerationTaskService taskService, TitleLibraryService titleLibraryService) {
        this.taskService = taskService;
        this.titleLibraryService = titleLibraryService;
    }

    @GetMapping
    public Result<Map<String, Object>> listTasks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        List<TitleGenerationTask> list = taskService.listTasks(keyword, status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", list.size());
        return Result.ok(result);
    }

    @GetMapping("/{id}")
    public Result<TitleGenerationTask> getTask(@PathVariable String id) {
        TitleGenerationTask task = taskService.findById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        return Result.ok(task);
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancelTask(@PathVariable String id) {
        TitleGenerationTask task = taskService.findById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        boolean success = taskService.cancelTask(id);
        if (!success) {
            return Result.error("只能取消排队中的任务");
        }
        titleLibraryService.updateGenerateStatus(task.getTitleLibraryId(), 0);
        log.info("[TaskController] 任务已取消: id={}, titleLibraryId={}", id, task.getTitleLibraryId());
        return Result.ok(null);
    }

    @PostMapping("/{id}/stop")
    public Result<Void> stopTask(@PathVariable String id) {
        TitleGenerationTask task = taskService.findById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        boolean success = taskService.stopTask(id);
        if (!success) {
            return Result.error("只能停止进行中的任务");
        }
        titleLibraryService.updateGenerateStatus(task.getTitleLibraryId(), 0);
        log.info("[TaskController] 任务已停止: id={}, titleLibraryId={}", id, task.getTitleLibraryId());
        return Result.ok(null);
    }

    @PostMapping("/{id}/retry")
    public Result<Map<String, Object>> retryTask(@PathVariable String id) {
        TitleGenerationTask newTask = taskService.retryTask(id);
        if (newTask == null) {
            return Result.error("原任务不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", newTask.getId());
        result.put("status", "pending");
        result.put("message", "重跑任务已创建");
        log.info("[TaskController] 任务已重跑: oldId={}, newId={}", id, newTask.getId());
        return Result.ok(result);
    }
}
