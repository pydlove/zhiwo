package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.TitleGenerationTask;
import com.example.blogger.entity.TitleLibrary;
import com.example.blogger.entity.User;
import com.example.blogger.service.TitleGenerationTaskService;
import com.example.blogger.service.TitleLibraryService;
import com.example.blogger.service.UserService;
import com.example.blogger.util.AiFlavorRemover;
import com.example.blogger.util.DocxGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final TitleGenerationTaskService taskService;
    private final TitleLibraryService titleLibraryService;
    private final UserService userService;
    private final DocxGenerator docxGenerator;
    private final AiFlavorRemover aiFlavorRemover;

    @Value("${app.script.replace-periods-path:}")
    private String replacePeriodsScriptPath;

    public TaskController(TitleGenerationTaskService taskService,
                          TitleLibraryService titleLibraryService,
                          UserService userService,
                          DocxGenerator docxGenerator,
                          AiFlavorRemover aiFlavorRemover) {
        this.taskService = taskService;
        this.titleLibraryService = titleLibraryService;
        this.userService = userService;
        this.docxGenerator = docxGenerator;
        this.aiFlavorRemover = aiFlavorRemover;
    }

    @GetMapping
    public Result<Map<String, Object>> listTasks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        int limit = Math.max(1, Math.min(100, pageSize));
        int offset = Math.max(0, (page - 1) * limit);
        List<TitleGenerationTask> list = taskService.listTasks(keyword, status, limit, offset);
        int total = taskService.countTasks(keyword, status);
        LocalDateTime now = LocalDateTime.now();
        for (TitleGenerationTask task : list) {
            if ("pending".equals(task.getStatus())) {
                task.setDuration("-");
                continue;
            }
            LocalDateTime start = task.getProcessStartedAt();
            if (start == null) {
                start = task.getCreatedAt();
            }
            LocalDateTime end = task.getProcessedAt();
            if (end == null) {
                end = now;
            }
            if (start != null && end != null) {
                long seconds = Duration.between(start, end).getSeconds();
                task.setDuration(formatDuration(seconds));
            } else {
                task.setDuration("-");
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", limit);
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

    @PostMapping("/{id}/rerun-backend")
    public Result<Void> rerunBackend(@PathVariable String id) {
        TitleGenerationTask task = taskService.findById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        if (task.getGeneratedContent() == null || task.getGeneratedContent().isEmpty()) {
            return Result.error("任务没有保存生成的内容，请重新创建任务");
        }
        try {
            String content = task.getGeneratedContent();
            log.info("[TaskController] 重跑后半段, title={}, contentLength={}", task.getTitle(), content.length());

            // 1. 去AI味
            String cleanedContent = aiFlavorRemover.removeAiFlavor(content);
            log.info("[TaskController] 去AI味完成, originalLength={}, cleanedLength={}", content.length(), cleanedContent.length());

            // 2. 生成DOCX
            String safeTitle = task.getTitle() != null ? task.getTitle() : "untitled";
            safeTitle = safeTitle.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", "").trim();
            safeTitle = safeTitle.replaceAll("\\s+", "，");
            if (safeTitle.isEmpty()) {
                safeTitle = "article_" + task.getTitleLibraryId();
            }
            String fileName = safeTitle + "_" + task.getId() + ".docx";
            String articlesDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "articles";
            File dir = new File(articlesDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String filePath = articlesDir + File.separator + fileName;

            // 获取用户主题色和字号配置
            String themeColor = null;
            Integer titleFontSize = null;
            Integer contentFontSize = null;
            try {
                TitleLibrary titleLib = titleLibraryService.getById(task.getTitleLibraryId());
                if (titleLib != null && titleLib.getRecommendUserId() != null) {
                    User user = userService.getById(titleLib.getRecommendUserId());
                    if (user != null) {
                        if (user.getThemeColor() != null && !user.getThemeColor().isEmpty()) {
                            themeColor = user.getThemeColor();
                        }
                        titleFontSize = user.getTitleFontSize();
                        contentFontSize = user.getContentFontSize();
                    }
                }
            } catch (Exception e) {
                log.warn("[TaskController] 获取用户样式配置失败，使用默认配置: {}", e.getMessage());
            }

            docxGenerator.generateDocx(task.getTitle(), cleanedContent, filePath, themeColor, titleFontSize, contentFontSize);
            String fileUrl = "/uploads/articles/" + fileName;
            taskService.updateStatus(task.getId(), "completed", fileUrl);
            titleLibraryService.updateGeneratedFile(task.getTitleLibraryId(), fileUrl, fileName);
            log.info("[TaskController] 重跑后半段完成: fileUrl={}", fileUrl);
            return Result.ok(null);
        } catch (Exception e) {
            log.error("[TaskController] 重跑后半段失败: id={}, error={}", id, e.getMessage(), e);
            return Result.error("重跑后半段失败: " + e.getMessage());
        }
    }

    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分" + (seconds % 60) + "秒";
        } else {
            return (seconds / 3600) + "小时" + ((seconds % 3600) / 60) + "分";
        }
    }
}
