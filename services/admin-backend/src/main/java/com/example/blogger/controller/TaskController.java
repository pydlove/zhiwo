package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.TitleGenerationTask;
import com.example.blogger.service.TitleGenerationTaskService;
import com.example.blogger.service.TitleLibraryService;
import com.example.blogger.util.DocxGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    private final DocxGenerator docxGenerator;

    @Value("${app.script.replace-periods-path:}")
    private String replacePeriodsScriptPath;

    public TaskController(TitleGenerationTaskService taskService,
                          TitleLibraryService titleLibraryService,
                          DocxGenerator docxGenerator) {
        this.taskService = taskService;
        this.titleLibraryService = titleLibraryService;
        this.docxGenerator = docxGenerator;
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

    @PostMapping("/{id}/regenerate-docx")
    public Result<Void> regenerateDocx(@PathVariable String id) {
        TitleGenerationTask task = taskService.findById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        if (task.getGeneratedContent() == null || task.getGeneratedContent().isEmpty()) {
            return Result.error("任务没有保存生成的内容，请重新创建任务");
        }
        try {
            String content = task.getGeneratedContent();
            log.info("[TaskController] 重新生成DOCX, title={}, contentLength={}", task.getTitle(), content.length());

            String safeTitle = task.getTitle() != null ? task.getTitle() : "untitled";
            safeTitle = safeTitle.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", "").trim();
            safeTitle = safeTitle.replaceAll("\\s+", "，");
            if (safeTitle.isEmpty()) {
                safeTitle = "article_" + task.getTitleLibraryId();
            }
            String fileName = safeTitle + ".docx";
            String articlesDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "articles";
            File dir = new File(articlesDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String filePath = articlesDir + File.separator + fileName;

            docxGenerator.generateDocx(task.getTitle(), content, filePath);
            String fileUrl = "/uploads/articles/" + fileName;
            taskService.updateStatus(task.getId(), "completed", fileUrl);
            titleLibraryService.updateGeneratedFile(task.getTitleLibraryId(), fileUrl, fileName);
            log.info("[TaskController] DOCX重新生成完成: fileUrl={}", fileUrl);
            return Result.ok(null);
        } catch (Exception e) {
            log.error("[TaskController] 重新生成DOCX失败: id={}, error={}", id, e.getMessage(), e);
            return Result.error("重新生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/reapply-ai-flavor")
    public Result<Void> reapplyAiFlavor(@PathVariable String id) {
        TitleGenerationTask task = taskService.findById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        if (task.getResultFileName() == null || task.getResultFileName().isEmpty()) {
            return Result.error("任务没有生成文件");
        }
        File scriptFile = new File(replacePeriodsScriptPath);
        if (!scriptFile.exists()) {
            return Result.error("去AI味脚本不存在");
        }

        String fileName = task.getResultFileName();
        String articlesDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "articles";
        String filePath = articlesDir + File.separator + fileName;

        try {
            execAiFlavorScript(filePath);
            log.info("[TaskController] 去AI味重新执行完成: id={}, file={}", id, filePath);
            return Result.ok(null);
        } catch (Exception e) {
            log.error("[TaskController] 重新执行去AI味失败: id={}, error={}", id, e.getMessage(), e);
            return Result.error("去AI味执行失败: " + e.getMessage());
        }
    }

    private void execAiFlavorScript(String filePath) throws Exception {
        Exception lastException = null;
        for (String pythonCmd : new String[]{"python3", "python"}) {
            try {
                List<String> command = new ArrayList<>();
                command.add(pythonCmd);
                command.add(replacePeriodsScriptPath);
                command.add(filePath);

                log.info("[TaskController] 执行去AI味脚本: {}", command);
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                StringBuilder stdout = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                    }
                }

                boolean finished = process.waitFor(60, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new RuntimeException("去AI味脚本执行超时");
                }

                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    throw new RuntimeException("去AI味脚本执行失败: " + stdout.toString().trim());
                }
                log.info("[TaskController] 去AI味脚本执行成功: cmd={}, output={}", pythonCmd, stdout.toString().trim());
                return;
            } catch (Exception e) {
                lastException = e;
                log.warn("[TaskController] 使用 {} 执行去AI味脚本失败: {}", pythonCmd, e.getMessage());
            }
        }
        throw new RuntimeException("去AI味脚本无法执行，已尝试 python3 和 python", lastException);
    }
}
