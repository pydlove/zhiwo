package com.example.blogger.scheduler;

import com.example.blogger.entity.Config;
import com.example.blogger.entity.SubscriptionPost;
import com.example.blogger.entity.TitleGenerationTask;
import com.example.blogger.entity.TitleLibrary;
import com.example.blogger.entity.TitleRecommendation;
import com.example.blogger.entity.User;
import com.example.blogger.mapper.ConfigMapper;
import com.example.blogger.mapper.SubscriptionPostMapper;
import com.example.blogger.mapper.TitleGenerationTaskMapper;
import com.example.blogger.mapper.TitleRecommendationMapper;
import com.example.blogger.service.LLMService;
import com.example.blogger.service.TaskInterruptManager;
import com.example.blogger.service.TitleGenerationTaskService;
import com.example.blogger.service.TitleLibraryService;
import com.example.blogger.service.UserService;
import com.example.blogger.util.AiFlavorRemover;
import com.example.blogger.util.DocxGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文章生成任务定时处理器
 * 扫描 tu_title_generation_task 表中的 pending 任务，
 * 调用大模型生成文章，保存为 docx 文件，并更新关联数据。
 * 支持配置并发数，默认单线程执行。
 */
@Component
public class GenerationTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(GenerationTaskScheduler.class);
    private static final String CONCURRENCY_CONFIG_KEY = "generation_task_concurrency";
    private static final int DEFAULT_CONCURRENCY = 1;
    private static final int MAX_CONCURRENCY = 10;

    private final TitleGenerationTaskMapper taskMapper;
    private final TitleGenerationTaskService taskService;
    private final LLMService llmService;
    private final DocxGenerator docxGenerator;
    private final TitleLibraryService titleLibraryService;
    private final UserService userService;
    private final TaskInterruptManager interruptManager;
    private final AiFlavorRemover aiFlavorRemover;
    private final ConfigMapper configMapper;
    private final SubscriptionPostMapper subscriptionPostMapper;
    private final TitleRecommendationMapper titleRecommendationMapper;

    private final ExecutorService executorService;
    private final Set<String> runningTasks = ConcurrentHashMap.newKeySet();

    public GenerationTaskScheduler(TitleGenerationTaskMapper taskMapper,
                                   TitleGenerationTaskService taskService,
                                   LLMService llmService,
                                   DocxGenerator docxGenerator,
                                   TitleLibraryService titleLibraryService,
                                   UserService userService,
                                   TaskInterruptManager interruptManager,
                                   AiFlavorRemover aiFlavorRemover,
                                   ConfigMapper configMapper,
                                   SubscriptionPostMapper subscriptionPostMapper,
                                   TitleRecommendationMapper titleRecommendationMapper) {
        this.taskMapper = taskMapper;
        this.taskService = taskService;
        this.llmService = llmService;
        this.docxGenerator = docxGenerator;
        this.titleLibraryService = titleLibraryService;
        this.userService = userService;
        this.interruptManager = interruptManager;
        this.aiFlavorRemover = aiFlavorRemover;
        this.configMapper = configMapper;
        this.subscriptionPostMapper = subscriptionPostMapper;
        this.titleRecommendationMapper = titleRecommendationMapper;
        this.executorService = Executors.newFixedThreadPool(MAX_CONCURRENCY);
    }

    /**
     * 定时扫描 pending 任务，根据配置的并发数提交到线程池执行
     */
    @Scheduled(fixedDelay = 10000)
    public void processTasks() {
        int concurrency = getConcurrency();
        int availableSlots = concurrency - runningTasks.size();

        if (availableSlots <= 0) {
            log.debug("[GenerationTaskScheduler] 当前运行 {} 个任务，并发上限 {}，无可用槽位", runningTasks.size(), concurrency);
            return;
        }

        log.debug("[GenerationTaskScheduler] 当前运行 {} 个任务，并发上限 {}，本次可启动 {} 个新任务", runningTasks.size(), concurrency, availableSlots);

        for (int i = 0; i < availableSlots; i++) {
            TitleGenerationTask task = taskMapper.findOnePending();
            if (task == null) {
                log.debug("[GenerationTaskScheduler] 无 pending 任务，停止扫描");
                break;
            }

            int pendingCount = taskMapper.countByStatus("pending");
            log.info("[GenerationTaskScheduler] 开始处理任务: id={}, 标题: {}, 队列中还剩 {} 个 pending 任务", task.getId(), task.getTitle(), pendingCount);

            // 立即标记为 processing，防止被其他调度周期重复取
            taskService.updateStatus(task.getId(), "processing", null);
            taskService.updateProcessStartedAt(task.getId(), LocalDateTime.now());
            taskService.updateProgress(task.getId(), 1, "构建提示词...");
            titleLibraryService.updateGenerateStatus(task.getTitleLibraryId(), 2);

            runningTasks.add(task.getId());
            executorService.submit(() -> {
                try {
                    executeTask(task);
                } finally {
                    runningTasks.remove(task.getId());
                }
            });
        }
    }

    private int getConcurrency() {
        try {
            Config cfg = configMapper.findByKey(CONCURRENCY_CONFIG_KEY);
            if (cfg != null && cfg.getConfigValue() != null) {
                int val = Integer.parseInt(cfg.getConfigValue().trim());
                return Math.max(1, Math.min(MAX_CONCURRENCY, val));
            }
        } catch (Exception e) {
            log.warn("[GenerationTaskScheduler] 读取并发配置失败，使用默认值 {}", DEFAULT_CONCURRENCY);
        }
        return DEFAULT_CONCURRENCY;
    }

    private void executeTask(TitleGenerationTask task) {
        try {
            // Step 1: Build prompt
            checkStopped(task.getId());
            taskService.updateProgress(task.getId(), 1, "构建提示词完成，准备生成...");

            // Step 2: Call LLM
            checkStopped(task.getId());
            String articlePrompt = task.getPrompt();
            // 追加系统指令：禁止模型输出思考过程，避免内容被 <think> 标签包裹导致误删
            if (!articlePrompt.contains("<think>") && !articlePrompt.contains("thinking")) {
                articlePrompt += "\n\n【系统指令】请直接输出文章正文内容，不要输出任何思考过程，不要使用 <think>、<thinking>、<thought>、<reasoning> 等标签包裹内容。";
            }
            log.info("[GenerationTaskScheduler] 调用 LLM 生成内容, prompt length: {}", articlePrompt.length());
            taskService.updateProgress(task.getId(), 2, "大模型生成中...");
            interruptManager.register(task.getId(), Thread.currentThread());
            String content;
            boolean interrupted = false;
            try {
                content = llmService.generateContent(articlePrompt);
            } finally {
                interruptManager.unregister(task.getId());
                interrupted = Thread.interrupted(); // 清除并获取中断状态
            }
            if (interrupted) {
                throw new InterruptedException("任务被停止");
            }
            log.info("[GenerationTaskScheduler] LLM 返回内容长度: {}", content.length());

            // 先过滤 think 标签，避免二次请求大模型时 prompt 携带思考过程
            content = aiFlavorRemover.removeThinkingTags(content);
            log.info("[GenerationTaskScheduler] 过滤 think 标签后内容长度: {}", content.length());

            taskService.updateGeneratedContent(task.getId(), content);
            taskService.updateProgress(task.getId(), 2, "正文生成完成");

            // Step 2.5: Generate chapter titles based on content
            checkStopped(task.getId());
            taskService.updateProgress(task.getId(), 2, "生成章节标题中...");
            String titlePrompt = buildTitlePrompt(content);
            String titleResponse;
            interruptManager.register(task.getId(), Thread.currentThread());
            boolean titleInterrupted = false;
            try {
                titleResponse = llmService.generateContent(titlePrompt);
            } finally {
                interruptManager.unregister(task.getId());
                titleInterrupted = Thread.interrupted();
            }
            if (titleInterrupted) {
                throw new InterruptedException("任务被停止");
            }
            log.info("[GenerationTaskScheduler] 标题生成返回长度: {}", titleResponse.length());
            String mergedContent = mergeTitlesIntoContent(content, titleResponse);
            taskService.updateGeneratedContent(task.getId(), mergedContent);
            taskService.updateProgress(task.getId(), 2, "章节标题生成完成");

            // Step 3: Remove AI flavor
            checkStopped(task.getId());
            taskService.updateProgress(task.getId(), 3, "去除AI味中...");
            String cleanedContent = aiFlavorRemover.removeAiFlavor(mergedContent);
            taskService.updateProgress(task.getId(), 3, "去除AI味完成");
            taskService.updateGeneratedContent(task.getId(), cleanedContent);

            // Step 4: Generate DOCX file
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
            taskService.updateProgress(task.getId(), 4, "写入文件中...");

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
                log.warn("[GenerationTaskScheduler] 获取用户样式配置失败，使用默认配置: {}", e.getMessage());
            }

            docxGenerator.generateDocx(task.getTitle(), cleanedContent, filePath, themeColor, titleFontSize, contentFontSize);
            log.info("[GenerationTaskScheduler] DOCX 生成成功");
            taskService.updateProgress(task.getId(), 4, "文件写入完成");

            String fileUrl = "/uploads/articles/" + fileName;

            // Step 4.5: 创建 SubscriptionPost 并回写 subscription_post_id
            try {
                TitleLibrary titleLib = titleLibraryService.getById(task.getTitleLibraryId());
                if (titleLib != null) {
                    SubscriptionPost post = new SubscriptionPost();
                    post.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
                    post.setTitleLibraryId(titleLib.getId());
                    post.setTitle(titleLib.getTitle());
                    post.setFileUrl(fileUrl);
                    post.setFileName(fileName);
                    post.setStatus("已上架");
                    post.setUsed(0);
                    if (titleLib.getTrackId() != null && !titleLib.getTrackId().isEmpty()) {
                        post.setTrackId(titleLib.getTrackId());
                    }
                    if (titleLib.getRecommendUserId() != null && !titleLib.getRecommendUserId().isEmpty()) {
                        post.setUserId(titleLib.getRecommendUserId());
                    }
                    subscriptionPostMapper.insert(post);

                    // 回写 TitleRecommendation.subscription_post_id
                    TitleRecommendation rec = titleRecommendationMapper.findLatestByTitleId(titleLib.getId());
                    if (rec != null && (rec.getSubscriptionPostId() == null || rec.getSubscriptionPostId().isEmpty())) {
                        titleRecommendationMapper.updateSubscriptionPostId(rec.getId(), post.getId());
                        log.info("[GenerationTaskScheduler] 回写推荐记录 subscription_post_id: recId={}, postId={}", rec.getId(), post.getId());
                    }
                }
            } catch (Exception e) {
                log.warn("[GenerationTaskScheduler] 创建 SubscriptionPost 或回写失败，不影响任务完成: {}", e.getMessage());
            }

            // Step 5: Update task status
            taskMapper.updateCompleted(task.getId(), "completed", fileUrl, fileName, LocalDateTime.now(), LocalDateTime.now());
            taskService.updateProgress(task.getId(), 5, "已完成");

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
    }

    private void checkStopped(String taskId) throws InterruptedException {
        TitleGenerationTask current = taskMapper.findById(taskId);
        if (current != null && "stopped".equals(current.getStatus())) {
            throw new InterruptedException("任务已停止");
        }
    }

    /**
     * 构建第二步提示词：根据正文生成章节标题及插入位置
     */
    private String buildTitlePrompt(String content) {
        return "请根据以下文章内容，生成1~3个适合用作章节小标题的标题，并指出每个标题应该插在正文第几段之前。\n\n"
            + "要求：\n"
            + "1. 输出格式必须是纯JSON数组，不要加markdown代码块标记\n"
            + "2. 每个元素包含：\n"
            + "   - \"title\": 标题文字（不要加<h3>标签）\n"
            + "   - \"position\": 插入位置，表示该标题应放在第几段正文之前（从1开始计数）\n"
            + "3. 标题数量根据文章内容决定，1~3个\n"
            + "4. 只输出JSON数组，不要输出任何其他文字\n\n"
            + "示例：[{\"title\":\"为什么习惯如此重要\",\"position\":3},{\"title\":\"如何改变旧习惯\",\"position\":6}]\n\n"
            + "文章内容：\n" + content;
    }

    /**
     * 将 LLM 返回的标题 JSON 按 position 插入到正文对应段落之前
     */
    private String mergeTitlesIntoContent(String content, String titleResponse) {
        try {
            // 1. 提取 JSON 数组
            String json = extractJsonArray(titleResponse);
            if (json == null || json.isEmpty()) {
                log.warn("[GenerationTaskScheduler] 无法从标题响应中提取JSON，将标题放在文章最前面");
                return titleResponse + "\n\n" + content;
            }

            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> titles = mapper.readValue(json,
                mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            if (titles == null || titles.isEmpty()) {
                log.warn("[GenerationTaskScheduler] 标题JSON解析为空，跳过插入");
                return content;
            }

            // 2. 按 position 排序
            titles.sort(Comparator.comparingInt(t -> {
                Object pos = t.get("position");
                if (pos instanceof Number) return ((Number) pos).intValue();
                try { return Integer.parseInt(pos.toString()); } catch (Exception e) { return Integer.MAX_VALUE; }
            }));

            // 3. 将正文按段落分割
            String[] paragraphs = content.split("\n\n+");

            // 4. 构建新内容
            StringBuilder sb = new StringBuilder();
            int titleIndex = 0;
            for (int i = 0; i < paragraphs.length; i++) {
                // 在当前段落前插入所有 position == i+1 的标题
                while (titleIndex < titles.size()) {
                    Map<String, Object> t = titles.get(titleIndex);
                    Object pos = t.get("position");
                    int position;
                    try {
                        position = pos instanceof Number ? ((Number) pos).intValue() : Integer.parseInt(pos.toString());
                    } catch (Exception e) {
                        position = Integer.MAX_VALUE;
                    }
                    if (position == i + 1) {
                        String titleText = t.get("title") != null ? t.get("title").toString() : "";
                        if (!titleText.isEmpty()) {
                            sb.append("<h3>").append(titleText).append("</h3>\n\n");
                        }
                        titleIndex++;
                    } else {
                        break;
                    }
                }
                sb.append(paragraphs[i]);
                if (i < paragraphs.length - 1) {
                    sb.append("\n\n");
                }
            }

            // 5. 剩余标题放最后
            while (titleIndex < titles.size()) {
                Map<String, Object> t = titles.get(titleIndex);
                String titleText = t.get("title") != null ? t.get("title").toString() : "";
                if (!titleText.isEmpty()) {
                    sb.append("\n\n<h3>").append(titleText).append("</h3>");
                }
                titleIndex++;
            }

            return sb.toString();
        } catch (Exception e) {
            log.warn("[GenerationTaskScheduler] 标题插入失败，fallback到放在文章最前面: {}", e.getMessage());
            return titleResponse + "\n\n" + content;
        }
    }

    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[.*\\]", Pattern.DOTALL);

    private String extractJsonArray(String text) {
        if (text == null || text.isEmpty()) return null;
        // 尝试直接找到JSON数组（支持多行）
        Matcher matcher = JSON_ARRAY_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
