package com.example.blogger.scheduler;

import com.example.blogger.entity.Config;
import com.example.blogger.entity.TitleGenerationTask;
import com.example.blogger.entity.TitleLibrary;
import com.example.blogger.entity.TitleRecommendation;
import com.example.blogger.entity.Track;
import com.example.blogger.entity.User;
import com.example.blogger.mapper.ConfigMapper;
import com.example.blogger.mapper.TitleGenerationTaskMapper;
import com.example.blogger.mapper.TitleRecommendationMapper;
import com.example.blogger.mapper.TrackMapper;
import com.example.blogger.service.ContentCheckService;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
    private final TitleRecommendationMapper titleRecommendationMapper;
    private final com.example.blogger.mapper.ImageLibraryMapper imageLibraryMapper;
    private final TrackMapper trackMapper;
    private final ContentCheckService contentCheckService;

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
                                   TitleRecommendationMapper titleRecommendationMapper,
                                   com.example.blogger.mapper.ImageLibraryMapper imageLibraryMapper,
                                   TrackMapper trackMapper,
                                   ContentCheckService contentCheckService) {
        this.taskMapper = taskMapper;
        this.taskService = taskService;
        this.llmService = llmService;
        this.docxGenerator = docxGenerator;
        this.titleLibraryService = titleLibraryService;
        this.userService = userService;
        this.interruptManager = interruptManager;
        this.aiFlavorRemover = aiFlavorRemover;
        this.configMapper = configMapper;
        this.titleRecommendationMapper = titleRecommendationMapper;
        this.imageLibraryMapper = imageLibraryMapper;
        this.trackMapper = trackMapper;
        this.contentCheckService = contentCheckService;
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
        log.info("[GenerationTaskScheduler] ========== 任务开始: id={}, titleLibraryId={}, title={} ==========", task.getId(), task.getTitleLibraryId(), task.getTitle());
        long taskStartTime = System.currentTimeMillis();
        try {
            // Step 1: Build prompt
            log.info("[GenerationTaskScheduler] [任务{}] Step 1/6: 构建提示词", task.getId());
            checkStopped(task.getId());
            taskService.updateProgress(task.getId(), 1, "构建提示词完成，准备生成...");
            log.info("[GenerationTaskScheduler] [任务{}] Step 1/6: 提示词构建完成, prompt长度={}", task.getId(), task.getPrompt() != null ? task.getPrompt().length() : 0);

            // Step 2: Call LLM
            log.info("[GenerationTaskScheduler] [任务{}] Step 2/6: 调用大模型生成正文", task.getId());
            checkStopped(task.getId());
            String articlePrompt = task.getPrompt();
            // 追加系统指令：禁止模型输出思考过程，避免内容被 <think> 标签包裹导致误删
            if (!articlePrompt.contains("<think>") && !articlePrompt.contains("thinking")) {
                articlePrompt += "\n\n【系统指令】"
                    + "请直接输出文章正文内容，不要输出任何思考过程，不要复述用户要求，不要加任何前言或总结。"
                    + "禁止使用 <think>、<thinking>、<thought>、<reasoning> 等标签包裹内容。"
                    + "不要用英文分析任务，直接开始写中文文章。";
            }
            log.info("[GenerationTaskScheduler] [任务{}] Step 2/6: 调用LLM, prompt长度={}", task.getId(), articlePrompt.length());
            long llmStart = System.currentTimeMillis();
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
            log.info("[GenerationTaskScheduler] [任务{}] Step 2/6: LLM返回完成, 耗时{}ms, 内容长度={}", task.getId(), System.currentTimeMillis() - llmStart, content.length());

            // 先过滤 think 标签，避免二次请求大模型时 prompt 携带思考过程
            content = aiFlavorRemover.removeThinkingTags(content);
            log.info("[GenerationTaskScheduler] [任务{}] Step 2/6: 过滤think标签后长度={}", task.getId(), content.length());

            taskService.updateGeneratedContent(task.getId(), content);
            taskService.updateProgress(task.getId(), 2, "正文生成完成");
            log.info("[GenerationTaskScheduler] [任务{}] Step 2/6: 正文生成完成", task.getId());

            // Step 2.5: Generate chapter titles based on content
            String mergedContent;
            String keyword;
            String[] paragraphs = content.split("\n\n+");
            log.info("[GenerationTaskScheduler] [任务{}] Step 2.5/6: 段落数={}, 判断是否需要生成章节标题", task.getId(), paragraphs.length);
            if (paragraphs.length <= 1) {
                log.info("[GenerationTaskScheduler] [任务{}] Step 2.5/6: 单段落文章, 跳过章节标题生成", task.getId());
                mergedContent = content;
                keyword = null;
                taskService.updateProgress(task.getId(), 2, "单段落文章，跳过标题生成");
            } else {
                checkStopped(task.getId());
                log.info("[GenerationTaskScheduler] [任务{}] Step 2.5/6: 开始生成章节标题", task.getId());
                taskService.updateProgress(task.getId(), 2, "生成章节标题中...");
                String titlePrompt = buildTitlePrompt(content);
                long titleLlmStart = System.currentTimeMillis();
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
                log.info("[GenerationTaskScheduler] [任务{}] Step 2.5/6: 标题LLM返回完成, 耗时{}ms, 长度={}", task.getId(), System.currentTimeMillis() - titleLlmStart, titleResponse.length());
                titleResponse = aiFlavorRemover.removeThinkingTags(titleResponse);
                log.info("[GenerationTaskScheduler] [任务{}] Step 2.5/6: 过滤think后标题长度={}", task.getId(), titleResponse.length());
                TitleMergeResult mergeResult = mergeTitlesIntoContent(content, titleResponse);
                mergedContent = mergeResult.content;
                keyword = mergeResult.keyword;
                log.info("[GenerationTaskScheduler] [任务{}] Step 2.5/6: 标题合并完成, keyword={}, 合并后长度={}", task.getId(), keyword, mergedContent.length());
                taskService.updateProgress(task.getId(), 2, "章节标题生成完成");
            }
            taskService.updateGeneratedContent(task.getId(), mergedContent);

            // Step 3: Remove AI flavor
            log.info("[GenerationTaskScheduler] [任务{}] Step 3/6: 开始去除AI味", task.getId());
            checkStopped(task.getId());
            taskService.updateProgress(task.getId(), 3, "去除AI味中...");
            long aiFlavorStart = System.currentTimeMillis();
            String cleanedContent = aiFlavorRemover.removeAiFlavor(mergedContent);
            log.info("[GenerationTaskScheduler] [任务{}] Step 3/6: 去除AI味完成, 耗时{}ms, 处理后长度={}", task.getId(), System.currentTimeMillis() - aiFlavorStart, cleanedContent.length());
            taskService.updateProgress(task.getId(), 3, "去除AI味完成");

            // Step 3.5: Insert image (keyword download first, fallback to image library)
            log.info("[GenerationTaskScheduler] [任务{}] Step 3.5/6: 开始插入图片, keyword={}", task.getId(), keyword);
            try {
                TitleLibrary titleLib = titleLibraryService.getById(task.getTitleLibraryId());
                if (titleLib != null && titleLib.getTrackId() != null && !titleLib.getTrackId().isEmpty()) {
                    com.example.blogger.entity.ImageLibrary image = null;

                    // 1. 优先根据关键字从百度下载图片
                    if (keyword != null && !keyword.isEmpty()) {
                        Track track = trackMapper.findById(titleLib.getTrackId());
                        if (track != null) {
                            log.info("[GenerationTaskScheduler] [任务{}] Step 3.5/6: 尝试关键字下载图片, keyword={}, track={}", task.getId(), keyword, track.getName());
                            image = downloadImageByKeyword(keyword, titleLib.getTrackId(), track.getName());
                            if (image != null) {
                                log.info("[GenerationTaskScheduler] [任务{}] Step 3.5/6: 关键字下载图片成功, url={}", task.getId(), image.getUrl());
                            } else {
                                log.info("[GenerationTaskScheduler] [任务{}] Step 3.5/6: 关键字下载图片失败, 将回退到图片库", task.getId());
                            }
                        }
                    }

                    // 2. 如果下载失败，从图片库随机获取
                    if (image == null) {
                        log.info("[GenerationTaskScheduler] [任务{}] Step 3.5/6: 从图片库随机获取", task.getId());
                        image = imageLibraryMapper.findRandomByTrackId(titleLib.getTrackId());
                        if (image != null) {
                            log.info("[GenerationTaskScheduler] [任务{}] Step 3.5/6: 图片库获取成功, url={}", task.getId(), image.getUrl());
                        } else {
                            log.info("[GenerationTaskScheduler] [任务{}] Step 3.5/6: 图片库也未找到匹配图片", task.getId());
                        }
                    }

                    if (image != null) {
                        cleanedContent = insertImageIntoContent(cleanedContent, image);
                        log.info("[GenerationTaskScheduler] [任务{}] Step 3.5/6: 图片已插入到正文", task.getId());
                    }
                } else {
                    log.info("[GenerationTaskScheduler] [任务{}] Step 3.5/6: 标题库无赛道信息, 跳过图片插入", task.getId());
                }
            } catch (Exception e) {
                log.warn("[GenerationTaskScheduler] [任务{}] Step 3.5/6: 插入图片失败, 跳过: {}", task.getId(), e.getMessage());
            }
            taskService.updateGeneratedContent(task.getId(), cleanedContent);

            // Step 4: Generate DOCX file
            log.info("[GenerationTaskScheduler] [任务{}] Step 4/6: 开始生成DOCX文件", task.getId());
            checkStopped(task.getId());
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
                        log.info("[GenerationTaskScheduler] [任务{}] Step 4/6: 用户样式配置: themeColor={}, titleFontSize={}, contentFontSize={}", task.getId(), themeColor, titleFontSize, contentFontSize);
                    }
                }
            } catch (Exception e) {
                log.warn("[GenerationTaskScheduler] [任务{}] Step 4/6: 获取用户样式配置失败, 使用默认: {}", task.getId(), e.getMessage());
            }

            long docxStart = System.currentTimeMillis();
            docxGenerator.generateDocx(task.getTitle(), cleanedContent, filePath, themeColor, titleFontSize, contentFontSize);
            log.info("[GenerationTaskScheduler] [任务{}] Step 4/6: DOCX生成完成, 耗时{}ms, path={}", task.getId(), System.currentTimeMillis() - docxStart, filePath);
            taskService.updateProgress(task.getId(), 4, "文件写入完成");

            String fileUrl = "/uploads/articles/" + fileName;

            // Step 4.6: 违禁词/敏感词检测
            log.info("[GenerationTaskScheduler] [任务{}] Step 4.6/6: 开始违禁词检测", task.getId());
            try {
                long checkStart = System.currentTimeMillis();
                ContentCheckService.CheckResult checkResult = contentCheckService.checkContent(cleanedContent);
                ObjectMapper mapper = new ObjectMapper();
                String checkResultJson = mapper.writeValueAsString(checkResult);
                titleLibraryService.updateBannedWordCheckResult(task.getTitleLibraryId(), checkResultJson);
                log.info("[GenerationTaskScheduler] [任务{}] Step 4.6/6: 违禁词检测完成, 耗时{}ms, totalChars={}, matches={}",
                        task.getId(), System.currentTimeMillis() - checkStart, checkResult.getTotalChars(), checkResult.getMatches().size());
            } catch (Exception e) {
                log.warn("[GenerationTaskScheduler] [任务{}] Step 4.6/6: 违禁词检测失败, 不影响任务完成: {}", task.getId(), e.getMessage());
            }

            // Step 4.5: 更新 TitleLibrary.generated_file_url（废弃 SubscriptionPost，统一用 title_library 字段）
            log.info("[GenerationTaskScheduler] [任务{}] Step 4.5/6: 更新TitleLibrary文件关联", task.getId());
            try {
                TitleLibrary titleLib = titleLibraryService.getById(task.getTitleLibraryId());
                if (titleLib != null) {
                    titleLibraryService.updateGeneratedFile(titleLib.getId(), fileUrl, fileName);
                    log.info("[GenerationTaskScheduler] [任务{}] Step 4.5/6: TitleLibrary文件关联更新成功", task.getId());
                }
            } catch (Exception e) {
                log.warn("[GenerationTaskScheduler] [任务{}] Step 4.5/6: 更新TitleLibrary文件关联失败, 不影响任务完成: {}", task.getId(), e.getMessage());
            }

            // Step 5: Update task status
            log.info("[GenerationTaskScheduler] [任务{}] Step 5/6: 更新任务状态为completed", task.getId());
            taskMapper.updateCompleted(task.getId(), "completed", fileUrl, fileName, LocalDateTime.now(), LocalDateTime.now());
            taskService.updateProgress(task.getId(), 5, "已完成");

            // Step 6: Update TitleLibrary association
            log.info("[GenerationTaskScheduler] [任务{}] Step 6/6: 更新TitleLibrary关联文件", task.getId());
            titleLibraryService.updateGeneratedFile(task.getTitleLibraryId(), fileUrl, fileName);

            // Step 7: 自动生成贴图（异步，失败不影响任务完成）
            log.info("[GenerationTaskScheduler] [任务{}] Step 7/6: 自动生成文章贴图", task.getId());
            try {
                java.util.List<String> images = titleLibraryService.generateImagePosts(task.getTitleLibraryId(), null, null, null);
                log.info("[GenerationTaskScheduler] [任务{}] 贴图生成成功, 共{}张", task.getId(), images.size());
            } catch (Exception e) {
                log.warn("[GenerationTaskScheduler] [任务{}] 贴图生成失败, 不影响任务完成: {}", task.getId(), e.getMessage());
            }

            long totalTime = System.currentTimeMillis() - taskStartTime;
            log.info("[GenerationTaskScheduler] ========== 任务完成: id={}, fileUrl={}, 总耗时{}ms ==========", task.getId(), fileUrl, totalTime);

        } catch (InterruptedException ie) {
            log.warn("[GenerationTaskScheduler] [任务{}] 任务被停止", task.getId());
            taskService.updateProgress(task.getId(), task.getProgressStep(), "已停止");
            titleLibraryService.updateGenerateStatus(task.getTitleLibraryId(), 0);
        } catch (Exception e) {
            log.error("[GenerationTaskScheduler] [任务{}] 任务处理失败: error={}", task.getId(), e.getMessage(), e);
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
     * 标题合并结果
     */
    private static class TitleMergeResult {
        final String content;
        final String keyword;

        TitleMergeResult(String content, String keyword) {
            this.content = content;
            this.keyword = keyword;
        }
    }

    /**
     * 构建第二步提示词：根据正文生成章节标题及插入位置，同时提取文章关键词
     */
    private String buildTitlePrompt(String content) {
        return "请根据以下文章内容，生成1~3个适合用作章节小标题的标题，并指出每个标题应该插在正文第几段之前。同时给出一个总结文章主题的关键词（用于搜索配图），关键词尽量简洁，不超过10个字。\n\n"
            + "要求：\n"
            + "1. 输出格式必须是纯JSON对象，不要加markdown代码块标记\n"
            + "2. JSON对象包含两个字段：\n"
            + "   - \"titles\": JSON数组，每个元素包含：\n"
            + "       - \"title\": 标题文字（不要加<h3>标签）\n"
            + "       - \"position\": 插入位置，表示该标题应放在第几段正文之前（从1开始计数）\n"
            + "   - \"keyword\": 文章主题关键词，用于搜索配图\n"
            + "3. 标题数量根据文章内容决定，1~3个\n"
            + "4. 只输出JSON对象，不要输出任何其他文字\n\n"
            + "示例：{\"titles\":[{\"title\":\"为什么习惯如此重要\",\"position\":3},{\"title\":\"如何改变旧习惯\",\"position\":6}],\"keyword\":\"习惯养成\"}\n\n"
            + "文章内容：\n" + content;
    }

    /**
     * 将 LLM 返回的标题 JSON 按 position 插入到正文对应段落之前，同时提取关键词
     */
    private TitleMergeResult mergeTitlesIntoContent(String content, String titleResponse) {
        try {
            // 1. 提取 JSON
            String json = extractJson(titleResponse);
            if (json == null || json.isEmpty()) {
                log.warn("[GenerationTaskScheduler] 无法从标题响应中提取JSON，将标题放在文章最前面");
                return new TitleMergeResult(titleResponse + "\n\n" + content, null);
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> root = mapper.readValue(json, Map.class);
            if (root == null) {
                log.warn("[GenerationTaskScheduler] 标题JSON解析为空，跳过插入");
                return new TitleMergeResult(content, null);
            }

            String keyword = root.get("keyword") != null ? root.get("keyword").toString().trim() : null;
            Object titlesObj = root.get("titles");
            List<Map<String, Object>> titles = new ArrayList<>();
            if (titlesObj instanceof List) {
                titles = (List<Map<String, Object>>) titlesObj;
            }
            if (titles.isEmpty()) {
                log.warn("[GenerationTaskScheduler] 标题数组为空，跳过插入");
                return new TitleMergeResult(content, keyword);
            }

            // 2. 按 position 排序并去重（同一位置只保留第一个）
            titles.sort(Comparator.comparingInt(t -> {
                Object pos = t.get("position");
                if (pos instanceof Number) return ((Number) pos).intValue();
                try { return Integer.parseInt(pos.toString()); } catch (Exception e) { return Integer.MAX_VALUE; }
            }));
            List<Map<String, Object>> deduped = new ArrayList<>();
            int lastPos = -1;
            for (Map<String, Object> t : titles) {
                Object pos = t.get("position");
                int p;
                try {
                    p = pos instanceof Number ? ((Number) pos).intValue() : Integer.parseInt(pos.toString());
                } catch (Exception e) {
                    p = Integer.MAX_VALUE;
                }
                if (p != lastPos) {
                    deduped.add(t);
                    lastPos = p;
                }
            }
            titles = deduped;

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

            // 5. 剩余标题（position 超出正文段落总数的）直接丢弃，不追加到末尾
            if (titleIndex < titles.size()) {
                int skipped = titles.size() - titleIndex;
                log.info("[GenerationTaskScheduler] 标题插入: 丢弃 {} 个超出段落范围的标题", skipped);
            }

            return new TitleMergeResult(sb.toString(), keyword);
        } catch (Exception e) {
            log.warn("[GenerationTaskScheduler] 标题插入失败，fallback到放在文章最前面: {}", e.getMessage());
            return new TitleMergeResult(titleResponse + "\n\n" + content, null);
        }
    }

    private static final Pattern JSON_PATTERN = Pattern.compile("[\\{\\[].*[\\}\\]]", Pattern.DOTALL);

    private String extractJson(String text) {
        if (text == null || text.isEmpty()) return null;
        Matcher matcher = JSON_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    /**
     * 将图片标记随机插入到文章前50%的某个普通段落之后（独占一个段落，前后换行）
     */
    private String insertImageIntoContent(String content, com.example.blogger.entity.ImageLibrary image) {
        if (content == null || content.isEmpty() || image == null || image.getUrl() == null) {
            return content;
        }
        String[] paragraphs = content.split("\n\n+");
        if (paragraphs.length == 0) return content;

        int half = Math.max(1, (paragraphs.length + 1) / 2);
        // 收集前50%中的普通段落索引（排除标题和空段落）
        List<Integer> normalIndices = new ArrayList<>();
        for (int i = 0; i < half; i++) {
            String p = paragraphs[i].trim();
            if (!p.isEmpty() && !p.startsWith("<h3>") && !p.startsWith("<h1>")) {
                normalIndices.add(i);
            }
        }
        if (normalIndices.isEmpty()) {
            // 退而求其次，找任意非空段落
            for (int i = 0; i < half; i++) {
                if (!paragraphs[i].trim().isEmpty()) {
                    normalIndices.add(i);
                    break;
                }
            }
        }
        if (normalIndices.isEmpty()) return content;

        int targetIndex = normalIndices.get((int) (Math.random() * normalIndices.size()));
        String imgTag = "<img src=\"" + image.getUrl() + "\">";

        // 在目标段落之后插入一个独立的图片段落
        List<String> newParagraphs = new ArrayList<>(Arrays.asList(paragraphs));
        newParagraphs.add(targetIndex + 1, imgTag);
        return String.join("\n\n", newParagraphs);
    }

    /**
     * 根据关键字调用 Python 脚本从百度下载图片，成功则保存到图片库并返回。
     * 只接受 baidu- 开头的图片（排除 picsum 回退）。
     */
    private com.example.blogger.entity.ImageLibrary downloadImageByKeyword(String keyword, String trackId, String trackName) {
        String scriptPath = resolveScriptPath();
        if (scriptPath == null) {
            log.warn("[GenerationTaskScheduler] 找不到下载脚本");
            return null;
        }

        String tempDirName = java.util.UUID.randomUUID().toString().replace("-", "");
        Path tempOutputDir = Paths.get(System.getProperty("user.dir"), "uploads", "temp_downloads", tempDirName);
        try {
            Files.createDirectories(tempOutputDir);
        } catch (Exception e) {
            log.warn("[GenerationTaskScheduler] 创建临时下载目录失败: {}", e.getMessage());
            return null;
        }

        List<String> command = new ArrayList<>();
        command.add("python3");
        command.add(scriptPath);
        command.add(tempOutputDir.toString());
        command.add("--count");
        command.add("1");
        command.add("--source");
        command.add("baidu");
        command.add("--category");
        command.add(trackName);
        command.add("--keyword");
        command.add(keyword);

        log.info("[GenerationTaskScheduler] 执行关键字图片下载: {}", String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("[GenerationTaskScheduler] 关键字图片下载超时");
                return null;
            }

            int exitCode = process.exitValue();
            String stdout = output.toString();
            if (exitCode != 0) {
                log.warn("[GenerationTaskScheduler] 关键字图片下载脚本失败, exitCode={}, stdout={}", exitCode, stdout);
                return null;
            }

            // 查找 baidu- 开头的图片文件（排除 picsum 回退）
            Path categoryDir = tempOutputDir.resolve(trackName);
            if (!Files.exists(categoryDir) || !Files.isDirectory(categoryDir)) {
                log.warn("[GenerationTaskScheduler] 下载目录不存在: {}", categoryDir);
                return null;
            }

            Path targetImage = null;
            try (Stream<Path> paths = Files.list(categoryDir)) {
                targetImage = paths
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.startsWith("baidu-") && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"));
                    })
                    .findFirst()
                    .orElse(null);
            }

            if (targetImage == null) {
                log.warn("[GenerationTaskScheduler] 未找到百度来源的图片文件，可能搜索无结果或回退到了picsum");
                return null;
            }

            // 复制到 uploads/images/
            String uploadDir = System.getProperty("user.dir") + "/uploads/images/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String ext = "";
            String originalName = targetImage.getFileName().toString();
            if (originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            } else {
                ext = ".jpg";
            }
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            int randomNum = (int) (Math.random() * 10000);
            String newName = "baidu-" + timestamp + String.format("%04d", randomNum) + ext;

            // 极小概率重名检查
            com.example.blogger.entity.ImageLibrary existing = imageLibraryMapper.findByName(newName);
            if (existing != null) {
                newName = "baidu-" + timestamp + String.format("%04d", (int) (Math.random() * 10000)) + ext;
            }

            String fileName = java.util.UUID.randomUUID().toString().replace("-", "") + ext;
            Path destPath = uploadPath.resolve(fileName);
            Files.copy(targetImage, destPath, StandardCopyOption.REPLACE_EXISTING);

            com.example.blogger.entity.ImageLibrary image = new com.example.blogger.entity.ImageLibrary();
            image.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
            image.setName(newName);
            image.setUrl("/uploads/images/" + fileName);
            image.setCategories(new ObjectMapper().writeValueAsString(Collections.singletonList(trackId)));
            imageLibraryMapper.insert(image);

            log.info("[GenerationTaskScheduler] 关键字图片下载成功: name={}, url={}", newName, image.getUrl());
            return image;
        } catch (Exception e) {
            log.warn("[GenerationTaskScheduler] 关键字图片下载异常: {}", e.getMessage());
            return null;
        } finally {
            // 清理临时目录
            try {
                deleteDirectory(tempOutputDir);
            } catch (Exception e) {
                log.warn("[GenerationTaskScheduler] 清理临时目录失败: {}", e.getMessage());
            }
        }
    }

    private String resolveScriptPath() {
        Path directPath = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "py", "download_category_images.py");
        if (Files.exists(directPath)) {
            return directPath.toString();
        }
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("py/download_category_images.py")) {
            if (is != null) {
                Path tempScript = Files.createTempFile("download_category_images", ".py");
                Files.copy(is, tempScript, StandardCopyOption.REPLACE_EXISTING);
                tempScript.toFile().deleteOnExit();
                return tempScript.toString();
            }
        } catch (Exception e) {
            log.warn("[GenerationTaskScheduler] 从 classpath 读取脚本失败: {}", e.getMessage());
        }
        return null;
    }

    private void deleteDirectory(Path dir) throws Exception {
        if (!Files.exists(dir)) return;
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (Exception e) {
                    log.warn("[GenerationTaskScheduler] 删除文件失败: {}", e.getMessage());
                }
            });
        }
    }
}
