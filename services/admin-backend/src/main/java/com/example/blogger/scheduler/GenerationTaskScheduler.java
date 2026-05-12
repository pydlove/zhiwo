package com.example.blogger.scheduler;

import com.example.blogger.entity.TitleGenerationTask;
import com.example.blogger.mapper.TitleGenerationTaskMapper;
import com.example.blogger.service.LLMService;
import com.example.blogger.service.TaskInterruptManager;
import com.example.blogger.service.TitleGenerationTaskService;
import com.example.blogger.service.TitleLibraryService;
import com.example.blogger.util.DocxGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 文章生成任务定时处理器
 * 扫描 tu_title_generation_task 表中的 pending 任务，
 * 调用大模型生成文章，保存为 docx 文件，并更新关联数据。
 */
@Component
public class GenerationTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(GenerationTaskScheduler.class);

    private final TitleGenerationTaskMapper taskMapper;
    private final TitleGenerationTaskService taskService;
    private final LLMService llmService;
    private final DocxGenerator docxGenerator;
    private final TitleLibraryService titleLibraryService;
    private final TaskInterruptManager interruptManager;

    @Value("${app.script.replace-periods-path:}")
    private String replacePeriodsScriptPath;

    private volatile boolean isProcessing = false;

    public GenerationTaskScheduler(TitleGenerationTaskMapper taskMapper,
                                   TitleGenerationTaskService taskService,
                                   LLMService llmService,
                                   DocxGenerator docxGenerator,
                                   TitleLibraryService titleLibraryService,
                                   TaskInterruptManager interruptManager) {
        this.taskMapper = taskMapper;
        this.taskService = taskService;
        this.llmService = llmService;
        this.docxGenerator = docxGenerator;
        this.titleLibraryService = titleLibraryService;
        this.interruptManager = interruptManager;
    }

    /**
     * 单线程串行执行：上一个任务完成后等待 10 秒，再扫描下一个任务
     */
    @Scheduled(fixedDelay = 10000)
    public void processTasks() {
        if (isProcessing) {
            return;
        }

        try {
            isProcessing = true;

            TitleGenerationTask task = taskMapper.findOnePending();
            if (task == null) {
                return;
            }

            int pendingCount = taskMapper.countByStatus("pending");
            log.info("[GenerationTaskScheduler] 开始处理任务: id={}, 标题: {}, 队列中还剩 {} 个 pending 任务", task.getId(), task.getTitle(), pendingCount);

            // 标记为 processing
            taskService.updateStatus(task.getId(), "processing", null);
            taskService.updateProgress(task.getId(), 1, "构建提示词...");
            titleLibraryService.updateGenerateStatus(task.getTitleLibraryId(), 2);

            try {
                // Step 1: Build prompt (already done during task creation, but track step)
                checkStopped(task.getId());
                taskService.updateProgress(task.getId(), 1, "构建提示词完成，准备生成...");

                // Step 2: Call LLM
                checkStopped(task.getId());
                log.info("[GenerationTaskScheduler] 调用 LLM 生成内容, prompt length: {}", task.getPrompt().length());
                taskService.updateProgress(task.getId(), 2, "大模型生成中...");
                interruptManager.register(task.getId(), Thread.currentThread());
                String content;
                boolean interrupted = false;
                try {
                    content = llmService.generateContent(task.getPrompt());
                } finally {
                    interruptManager.unregister(task.getId());
                    interrupted = Thread.interrupted(); // 清除并获取中断状态
                }
                if (interrupted) {
                    throw new InterruptedException("任务被停止");
                }
                log.info("[GenerationTaskScheduler] LLM 返回内容长度: {}", content.length());
                taskService.updateGeneratedContent(task.getId(), content);
                taskService.updateProgress(task.getId(), 2, "大模型生成完成");

                // Step 3: Generate DOCX file
                checkStopped(task.getId());
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

                log.info("[GenerationTaskScheduler] 生成 DOCX: {}", filePath);
                taskService.updateProgress(task.getId(), 3, "写入文件中...");
                docxGenerator.generateDocx(task.getTitle(), content, filePath);
                log.info("[GenerationTaskScheduler] DOCX 生成成功");
                taskService.updateProgress(task.getId(), 3, "文件写入完成");

                // Step 4: 样式优化（h3/s 标签已在 generateDocx 中处理，此处标记进度）
                taskService.updateProgress(task.getId(), 4, "样式优化完成");

                // Step 5: Remove AI flavor
                checkStopped(task.getId());
                if (replacePeriodsScriptPath != null && !replacePeriodsScriptPath.isEmpty()) {
                    File scriptFile = new File(replacePeriodsScriptPath);
                    if (scriptFile.exists()) {
                        taskService.updateProgress(task.getId(), 5, "去除AI味中...");
                        removeAiFlavor(filePath);
                        taskService.updateProgress(task.getId(), 5, "去除AI味完成");
                    } else {
                        log.warn("[GenerationTaskScheduler] 去AI味脚本不存在: {}", replacePeriodsScriptPath);
                    }
                }

                // Step 6: Update task status
                String fileUrl = "/uploads/articles/" + fileName;
                taskMapper.updateCompleted(task.getId(), "completed", fileUrl, fileName, LocalDateTime.now(), LocalDateTime.now());
                taskService.updateProgress(task.getId(), 6, "已完成");

                // Step 6: Update TitleLibrary association
                titleLibraryService.updateGeneratedFile(task.getTitleLibraryId(), fileUrl, fileName);
                log.info("[GenerationTaskScheduler] 任务完成: id={}, fileUrl={}", task.getId(), fileUrl);

            } catch (InterruptedException ie) {
                log.warn("[GenerationTaskScheduler] 任务被停止: id={}", task.getId());
                taskService.updateProgress(task.getId(), task.getProgressStep(), "已停止");
                titleLibraryService.updateGenerateStatus(task.getTitleLibraryId(), 0);
            } catch (Exception e) {
                log.error("[GenerationTaskScheduler] 任务处理失败: id={}, error={}", task.getId(), e.getMessage(), e);
                taskService.updateFailed(task.getId(), e.getMessage());
                titleLibraryService.updateGenerateStatus(task.getTitleLibraryId(), 0);
            }

        } finally {
            isProcessing = false;
        }
    }

    private void checkStopped(String taskId) throws InterruptedException {
        TitleGenerationTask current = taskMapper.findById(taskId);
        if (current != null && "stopped".equals(current.getStatus())) {
            throw new InterruptedException("任务已停止");
        }
    }

    private void removeAiFlavor(String filePath) {
        Exception lastException = null;
        for (String pythonCmd : new String[]{"python3", "python"}) {
            try {
                List<String> command = new ArrayList<>();
                command.add(pythonCmd);
                command.add(replacePeriodsScriptPath);
                command.add(filePath);

                log.info("[GenerationTaskScheduler] 尝试执行去AI味脚本: {}", command);
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
                    log.warn("[GenerationTaskScheduler] 去AI味脚本执行超时");
                    throw new RuntimeException("去AI味脚本执行超时");
                }

                int exitCode = process.exitValue();
                log.info("[GenerationTaskScheduler] 去AI味脚本执行完成 cmd={} exitCode={}", pythonCmd, exitCode);
                if (exitCode != 0) {
                    String errOutput = stdout.toString().trim();
                    log.error("[GenerationTaskScheduler] 去AI味脚本异常: {}", errOutput);
                    throw new RuntimeException("去AI味脚本执行失败: " + errOutput);
                }
                log.info("[GenerationTaskScheduler] 去AI味脚本输出: {}", stdout.toString().trim());
                return;
            } catch (Exception e) {
                lastException = e;
                log.warn("[GenerationTaskScheduler] 使用 {} 执行去AI味脚本失败: {}", pythonCmd, e.getMessage());
            }
        }
        throw new RuntimeException("去AI味脚本无法执行，已尝试 python3 和 python", lastException);
    }
}
