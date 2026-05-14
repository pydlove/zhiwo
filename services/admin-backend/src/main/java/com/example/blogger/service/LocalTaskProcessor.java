package com.example.blogger.service;

import com.example.blogger.entity.TitleGenerationTask;
import com.example.blogger.util.AiFlavorRemover;
import com.example.blogger.util.DocxGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Map;

/**
 * 本地任务处理器
 * 仅在本地的开发环境中运行，负责：
 * 1. 定时扫描服务器上的任务表
 * 2. 调用本地 Claude Code CLI 生成文章
 * 3. 生成 DOCX 文件并上传到服务器
 *
 * 通过配置 local.task-processor.enabled=true 启用
 */
@Component
@ConditionalOnProperty(name = "local.task-processor.enabled", havingValue = "true")
public class LocalTaskProcessor {

    private final TitleGenerationTaskService taskService;
    private final ClaudeCodeService claudeCodeService;
    private final DocxGenerator docxGenerator;
    private final AiFlavorRemover aiFlavorRemover;
    private final RestTemplate restTemplate;

    @Value("${local.task-processor.server-url:http://localhost:8080}")
    private String serverUrl;

    private volatile boolean isProcessing = false;

    @Autowired
    public LocalTaskProcessor(TitleGenerationTaskService taskService,
                              ClaudeCodeService claudeCodeService,
                              DocxGenerator docxGenerator,
                              AiFlavorRemover aiFlavorRemover,
                              RestTemplate restTemplate) {
        this.taskService = taskService;
        this.claudeCodeService = claudeCodeService;
        this.docxGenerator = docxGenerator;
        this.aiFlavorRemover = aiFlavorRemover;
        this.restTemplate = restTemplate;
    }

    /**
     * 每 5 秒扫描一次任务表，单线程执行
     */
    @Scheduled(fixedDelay = 5000)
    public void processTasks() {
        // 避免并发执行，确保单线程
        if (isProcessing) {
            return;
        }

        try {
            isProcessing = true;

            // 查询最老的一个 pending 任务
            TitleGenerationTask task = taskService.findOnePending();
            if (task == null) {
                return;
            }

            System.out.println("[LocalTaskProcessor] 发现任务: " + task.getId() + "，标题: " + task.getTitle());

            // 标记为 processing
            taskService.updateStatus(task.getId(), "processing", null);

            try {
                // Step 1: 调用本地 Claude Code CLI 生成内容
                System.out.println("[LocalTaskProcessor] 调用 Claude CLI 生成内容...");
                String content = claudeCodeService.callClaude(task.getPrompt());
                System.out.println("[LocalTaskProcessor] Claude CLI 返回内容长度: " + content.length());

                // Step 1.5: 去除 AI 味及思考标签
                String cleanedContent = aiFlavorRemover.removeAiFlavor(content);
                System.out.println("[LocalTaskProcessor] 去除 AI 味后内容长度: " + cleanedContent.length());

                // Step 2: 生成本地 DOCX 文件
                String fileName = "article_" + task.getTitleLibraryId() + "_" + System.currentTimeMillis() + ".docx";
                String tmpDirPath = System.getProperty("user.dir") + File.separator + "tmp";
                File tmpDir = new File(tmpDirPath);
                if (!tmpDir.exists()) {
                    tmpDir.mkdirs();
                }
                String localFilePath = tmpDirPath + File.separator + fileName;

                System.out.println("[LocalTaskProcessor] 生成 DOCX: " + localFilePath);
                docxGenerator.generateDocx(task.getTitle(), cleanedContent, localFilePath);

                // Step 3: 上传文件到服务器
                System.out.println("[LocalTaskProcessor] 上传文件到服务器...");
                uploadFileToServer(localFilePath, fileName, task.getId());

                // Step 4: 清理本地临时文件
                File localFile = new File(localFilePath);
                if (localFile.exists()) {
                    localFile.delete();
                }

                System.out.println("[LocalTaskProcessor] 任务完成: " + task.getId());

            } catch (Exception e) {
                System.err.println("[LocalTaskProcessor] 任务处理失败: " + task.getId() + ", 错误: " + e.getMessage());
                e.printStackTrace();
                taskService.updateFailed(task.getId(), e.getMessage());
            }

        } finally {
            isProcessing = false;
        }
    }

    /**
     * 通过 HTTP 接口上传文件到服务器
     */
    private void uploadFileToServer(String localFilePath, String fileName, String taskId) {
        String uploadUrl = serverUrl + "/api/title-library/upload-article";

        File file = new File(localFilePath);
        if (!file.exists()) {
            throw new RuntimeException("本地文件不存在: " + localFilePath);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));
        body.add("taskId", taskId);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity, Map.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("文件上传失败，状态码: " + response.getStatusCode());
        }

        Map<String, Object> result = response.getBody();
        if (result == null || !Boolean.TRUE.equals(result.get("success"))) {
            String msg = result != null && result.get("msg") != null ? result.get("msg").toString() : "未知错误";
            throw new RuntimeException("文件上传失败: " + msg);
        }

        System.out.println("[LocalTaskProcessor] 文件上传成功: " + result.get("data"));
    }
}
