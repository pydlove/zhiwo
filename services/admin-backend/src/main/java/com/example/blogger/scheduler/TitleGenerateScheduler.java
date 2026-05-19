package com.example.blogger.scheduler;

import com.example.blogger.entity.PromptTemplate;
import com.example.blogger.entity.TitleBannedWord;
import com.example.blogger.entity.TitleGenerateTask;
import com.example.blogger.entity.TitleLibrary;
import com.example.blogger.entity.Track;
import com.example.blogger.mapper.PromptTemplateMapper;
import com.example.blogger.mapper.TitleBannedWordMapper;
import com.example.blogger.mapper.TitleGenerateTaskMapper;
import com.example.blogger.mapper.TitleLibraryMapper;
import com.example.blogger.mapper.TrackMapper;
import com.example.blogger.service.ContentCheckService;
import com.example.blogger.service.LLMService;
import com.example.blogger.service.TitleGenerateTaskService;
import com.example.blogger.service.TitleLibraryService;
import com.example.blogger.service.TitleReviewService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.*;

/**
 * V2 标题生成任务定时处理器
 * 扫描 tu_title_generate_task 表中的 pending 任务，
 * 调用大模型（kimi/minimax）生成标题，保存到 tu_title_library 和 Excel 文件。
 */
@Component
public class TitleGenerateScheduler {

    private static final Logger log = LoggerFactory.getLogger(TitleGenerateScheduler.class);

    private final TitleGenerateTaskMapper taskMapper;
    private final TitleGenerateTaskService taskService;
    private final LLMService llmService;
    private final TitleLibraryService titleLibraryService;
    private final TitleReviewService titleReviewService;
    private final TrackMapper trackMapper;
    private final ContentCheckService contentCheckService;
    private final PromptTemplateMapper promptTemplateMapper;
    private final TitleBannedWordMapper titleBannedWordMapper;
    private final TitleLibraryMapper titleLibraryMapper;
    private final ObjectMapper objectMapper;

    private volatile boolean isProcessing = false;

    private static class SaveResult {
        final int savedCount;
        final int skipCount;
        SaveResult(int savedCount, int skipCount) {
            this.savedCount = savedCount;
            this.skipCount = skipCount;
        }
    }

    public TitleGenerateScheduler(TitleGenerateTaskMapper taskMapper,
                                  TitleGenerateTaskService taskService,
                                  LLMService llmService,
                                  TitleLibraryService titleLibraryService,
                                  TitleReviewService titleReviewService,
                                  TrackMapper trackMapper,
                                  ContentCheckService contentCheckService,
                                  PromptTemplateMapper promptTemplateMapper,
                                  TitleBannedWordMapper titleBannedWordMapper,
                                  TitleLibraryMapper titleLibraryMapper) {
        this.taskMapper = taskMapper;
        this.taskService = taskService;
        this.llmService = llmService;
        this.titleLibraryService = titleLibraryService;
        this.titleReviewService = titleReviewService;
        this.trackMapper = trackMapper;
        this.contentCheckService = contentCheckService;
        this.promptTemplateMapper = promptTemplateMapper;
        this.titleBannedWordMapper = titleBannedWordMapper;
        this.titleLibraryMapper = titleLibraryMapper;
        this.objectMapper = new ObjectMapper();
    }

    @Scheduled(fixedDelay = 10000)
    public void processTasks() {
        if (isProcessing) {
            return;
        }
        try {
            isProcessing = true;
            TitleGenerateTask task = taskMapper.findOnePending();
            if (task == null) {
                return;
            }
            int pendingCount = taskMapper.countByStatus("pending");
            log.info("[TitleGenerateScheduler] 开始处理任务: id={}, 队列中还剩 {} 个 pending 任务", task.getId(), pendingCount);

            taskService.updateStatus(task.getId(), "processing");
            processTask(task);
        } finally {
            isProcessing = false;
        }
    }

    private void processTask(TitleGenerateTask task) {
        try {
            // Step 1: 准备数据
            taskService.updateProgress(task.getId(), 1, "准备赛道数据...");
            List<String> selectedPlatforms = parseJsonArray(task.getPlatforms());
            List<String> selectedTrackIds = parseJsonArray(task.getTrackIds());
            int countPerCombo = task.getCountPerCombo() != null ? task.getCountPerCombo() : 3;
            String instruction = task.getInstruction();

            List<Track> allTracks = trackMapper.findAll();
            if (allTracks == null || allTracks.isEmpty()) {
                throw new RuntimeException("系统中没有赛道数据");
            }

            List<Track> tracks;
            if (selectedTrackIds != null && !selectedTrackIds.isEmpty()) {
                tracks = allTracks.stream()
                        .filter(t -> selectedTrackIds.contains(t.getId()))
                        .toList();
            } else {
                tracks = allTracks;
            }
            if (tracks.isEmpty()) {
                throw new RuntimeException("选择的赛道中没有可用数据");
            }

            List<String> platforms;
            if (selectedPlatforms != null && !selectedPlatforms.isEmpty()) {
                platforms = selectedPlatforms;
            } else {
                platforms = Arrays.asList("公众号", "今日头条", "百家号");
            }

            List<Map<String, String>> allRows = Collections.synchronizedList(new ArrayList<>());
            Map<String, String> trackNameToIdMap = new HashMap<>();

            for (String platform : platforms) {
                List<Track> platformTracks = tracks.stream()
                        .filter(t -> t.getPlatforms() != null && t.getPlatforms().contains(platform))
                        .toList();
                if (platformTracks.isEmpty()) continue;

                int batchSize = 5;
                for (int batchStart = 0; batchStart < platformTracks.size(); batchStart += batchSize) {
                    // 检查任务是否被取消
                    TitleGenerateTask current = taskMapper.findById(task.getId());
                    if (current != null && "stopped".equals(current.getStatus())) {
                        log.info("[TitleGenerateScheduler] 任务已停止: id={}", task.getId());
                        taskService.updateProgress(task.getId(), current.getProgressStep(), "已停止");
                        return;
                    }

                    int batchEnd = Math.min(batchStart + batchSize, platformTracks.size());
                    List<Track> batchTracks = platformTracks.subList(batchStart, batchEnd);

                    taskService.updateProgress(task.getId(), 2,
                            "大模型生成中：" + platform + "（批次 " + (batchStart / batchSize + 1) + "/" + (int) Math.ceil(platformTracks.size() / 5.0) + "）");

                    String prompt = buildPrompt(platform, batchTracks, countPerCombo, instruction, task);
                    log.info("[TitleGenerateScheduler] 调用LLM生成标题, prompt长度={}", prompt.length());
                    String llmResponse = llmService.generateContent(prompt);
                    log.info("[TitleGenerateScheduler] LLM返回长度={}", llmResponse.length());

                    JsonNode arr = extractJsonArray(llmResponse);
                    if (arr == null || !arr.isArray()) {
                        log.warn("[TitleGenerateScheduler] 无法解析LLM返回的JSON, response前200字={}",
                                llmResponse.substring(0, Math.min(200, llmResponse.length())));
                        continue;
                    }

                    Map<String, Integer> trackCountMap = new HashMap<>();
                    for (JsonNode node : arr) {
                        String title = node.path("title").asText("").trim();
                        if (title.isEmpty()) continue;
                        String rawTrack = node.path("track").asText("");
                        String cleanTrack = rawTrack.split("[：:（]", 2)[0].trim();
                        String description = node.path("description").asText("");

                        int trackCount = trackCountMap.getOrDefault(cleanTrack, 0);
                        if (trackCount < countPerCombo) {
                            Map<String, String> row = new HashMap<>();
                            row.put("title", title);
                            row.put("platform", platform);
                            row.put("track", cleanTrack);
                            row.put("description", description);
                            allRows.add(row);
                            trackCountMap.put(cleanTrack, trackCount + 1);
                        }
                        for (Track t : batchTracks) {
                            trackNameToIdMap.put(t.getName(), t.getId());
                        }
                    }
                }
            }

            log.info("[TitleGenerateScheduler] 共生成标题 {} 条", allRows.size());

            // Step 3: 解析入库
            taskService.updateProgress(task.getId(), 3, "解析入库中...");
            SaveResult saveResult = saveTitles(allRows, trackNameToIdMap, task.getId());
            log.info("[TitleGenerateScheduler] 入库完成: 保存 {} 条, 跳过重复 {} 条", saveResult.savedCount, saveResult.skipCount);

            // Step 4: 生成 Excel
            taskService.updateProgress(task.getId(), 4, "生成Excel文件...");
            String fileName = "生成标题_" + task.getId() + ".xlsx";
            String articlesDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "titles";
            File dir = new File(articlesDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String filePath = articlesDir + File.separator + fileName;
            writeExcel(allRows, filePath);
            String fileUrl = "/uploads/titles/" + fileName;
            log.info("[TitleGenerateScheduler] Excel生成完成: {}", filePath);

            // Step 5: 完成
            taskService.updateCompleted(task.getId(), fileUrl, fileName, saveResult.skipCount, saveResult.savedCount);
            taskService.updateProgress(task.getId(), 5, "已完成，共生成 " + allRows.size() + " 条，入库 " + saveResult.savedCount + " 条，重复 " + saveResult.skipCount + " 条");
            log.info("[TitleGenerateScheduler] 任务完成: id={}", task.getId());

        } catch (Exception e) {
            log.error("[TitleGenerateScheduler] 任务处理失败: id={}, error={}", task.getId(), e.getMessage(), e);
            taskService.updateFailed(task.getId(), e.getMessage());
        }
    }

    private String buildPrompt(String platform, List<Track> batchTracks, int countPerCombo, String instruction, TitleGenerateTask task) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的新媒体爆款标题创作者，请严格按照要求生成高质量标题。\n\n");
        prompt.append("目标平台：").append(platform).append("\n");
        prompt.append("需要生成标题的赛道：\n");
        boolean hasSocialTrack = false;
        List<String> batchTrackIds = new ArrayList<>();
        for (int i = 0; i < batchTracks.size(); i++) {
            Track t = batchTracks.get(i);
            prompt.append(i + 1).append(". ").append(t.getName());
            if (t.getIntro() != null && !t.getIntro().isEmpty()) {
                prompt.append("（").append(t.getIntro()).append("）");
            }
            prompt.append("\n");
            if (isSocialTrack(t.getName())) {
                hasSocialTrack = true;
            }
            if (t.getId() != null) {
                batchTrackIds.add(t.getId());
            }
        }

        // 注入标题风格模板
        String styleTemplateId = task != null ? task.getStyleTemplateId() : null;
        if (styleTemplateId != null && !styleTemplateId.isEmpty()) {
            try {
                PromptTemplate styleTemplate = promptTemplateMapper.findById(styleTemplateId);
                if (styleTemplate != null && styleTemplate.getContent() != null && !styleTemplate.getContent().isEmpty()) {
                    prompt.append("\n【标题风格要求（必须严格遵循）】\n");
                    prompt.append("风格名称：").append(styleTemplate.getName()).append("\n");
                    prompt.append(styleTemplate.getContent()).append("\n");
                }
            } catch (Exception e) {
                log.warn("[TitleGenerateScheduler] 查询风格模板失败: {}", styleTemplateId);
            }
        }

        // 注入禁用词
        try {
            List<TitleBannedWord> bannedWords = titleBannedWordMapper.findAllActive();
            if (bannedWords != null && !bannedWords.isEmpty()) {
                prompt.append("\n【绝对禁止使用的词汇和表述（标题和描述中均不得出现）】\n");
                for (TitleBannedWord bw : bannedWords) {
                    prompt.append("- ").append(bw.getWord());
                    if (bw.getCategory() != null && !bw.getCategory().isEmpty()) {
                        prompt.append("（").append(bw.getCategory()).append("）");
                    }
                    prompt.append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("[TitleGenerateScheduler] 查询禁用词失败: {}", e.getMessage());
        }

        // 注入近期历史标题（作为去重约束）
        if (!batchTrackIds.isEmpty()) {
            try {
                List<TitleLibrary> recentTitles = titleLibraryMapper.findRecentByTrackIds(batchTrackIds, 15);
                if (recentTitles != null && !recentTitles.isEmpty()) {
                    prompt.append("\n【近期已生成标题（严禁与以下标题在结构、用词、角度上雷同）】\n");
                    Set<String> shownTracks = new HashSet<>();
                    int showCount = 0;
                    for (TitleLibrary rt : recentTitles) {
                        if (showCount >= 15) break;
                        String trackName = rt.getTrackName() != null ? rt.getTrackName() : "未知赛道";
                        if (!shownTracks.contains(trackName)) {
                            prompt.append("\n").append(trackName).append("：\n");
                            shownTracks.add(trackName);
                        }
                        prompt.append("  - ").append(rt.getTitle()).append("\n");
                        showCount++;
                    }
                    prompt.append("\n要求：新标题必须与以上所有标题有明显差异，禁止换词重组、禁止同义改写、禁止仅调整语序。必须从全新的角度和切入点创作。\n");
                }
            } catch (Exception e) {
                log.warn("[TitleGenerateScheduler] 查询历史标题失败: {}", e.getMessage());
            }
        }

        prompt.append("\n每个赛道生成").append(countPerCombo).append("个标题。要求：\n");
        prompt.append("1. 标题是爆款风格，吸引眼球，适合").append(platform).append("传播，但不刻意标题党\n");
        prompt.append("2. 每个标题的 track 字段必须是上面给定的赛道名称（纯名称，不要包含括号内的说明），严禁自创赛道名称\n");
        prompt.append("3. 每个标题必须配一段文章写作思路（80-150字），要求：\n");
        prompt.append("   - 明确文章的核心观点和切入角度\n");
        prompt.append("   - 列出2-3个关键论据或故事线索，说明如何展开内容\n");
        prompt.append("   - 指明目标读者群体和阅读收益\n");
        prompt.append("   - 这段写作思路将直接用于后续文章的构思和撰写，必须具体、可执行，不能是空泛的概括\n");
        prompt.append("4. 所有生成的标题必须全局唯一，同一批次内不同赛道之间不得出现相同或高度相似的标题\n");
        prompt.append("5. 标题和描述中禁止出现英文双引号 \"，如有引用需求请使用中文引号「」或『』代替\n");
        prompt.append("6. 只输出纯JSON，不要markdown代码块，不要任何额外文字，不要在输出中包含任何思考过程\n");
        prompt.append("7. 禁止在输出中出现任何 <thinking>、<think>、<answer>、<think>> 等标签，不要包裹任何思考内容\n");
        prompt.append("8. 同一赛道内的多个标题之间必须有明显差异，从不同角度切入，角度包括但不限于：叙事视角、情感基调、读者角色、问题切入点、解决方案类型，避免标题结构和用词高度雷同\n");
        prompt.append("9. 新标题必须与「近期已生成标题」列表中的标题在语义结构、核心词汇、情感基调上保持显著差异，禁止仅替换个别词语或调整语序\n\n");
        prompt.append("格式：{\"titles\":[{\"track\":\"赛道名称\",\"title\":\"标题文字\",\"description\":\"SEO描述\"},...]}");
        if (instruction != null && !instruction.trim().isEmpty()) {
            prompt.append("\n\n【标题生成方向（必须严格遵循，多样化表达）】\n").append(instruction.trim());
        }
        return prompt.toString();
    }

    /**
     * 从 LLM 返回中提取 JSON 数组，支持多候选解析和强化的标签清理
     */
    private JsonNode extractJsonArray(String llmResponse) {
        // 策略1：标准清理（删除 think 标签及内容）
        JsonNode result = tryExtractJsonArrayInternal(llmResponse, true);
        if (result != null) return result;

        // 策略2：仅删除标签标记，保留标签内部内容（有些模型把 JSON 放在 think 标签内）
        log.info("[TitleGenerateScheduler] 标准清理未找到JSON，尝试保留think标签内容提取");
        result = tryExtractJsonArrayInternal(llmResponse, false);
        if (result != null) return result;

        log.warn("[TitleGenerateScheduler] 无法解析LLM返回, 文本前500字: {}",
                llmResponse.length() > 500 ? llmResponse.substring(0, 500) + "..." : llmResponse);
        return null;
    }

    private JsonNode tryExtractJsonArrayInternal(String rawText, boolean stripTagContent) {
        String text = rawText;
        // 1. 清理思维链标签
        if (stripTagContent) {
            // 删除标签及内部内容
            text = text.replaceAll("(?is)<(thinking|think|thought|reasoning|answer)\\b[^>]*>.*?</\\1>", " ");
            text = text.replaceAll("(?is)<(output|response|text)\\b[^>]*>.*?</\\1>", " ");
            text = text.replaceAll("(?si)<think>(.*?)</think>", " ");
            text = text.replaceAll("(?si)<think>.*", " ");
        } else {
            // 仅删除标签标记本身，保留内容（处理模型把JSON放在think内的情况）
            text = text.replaceAll("(?is)</?(thinking|think|thought|reasoning|answer|output|response|text)\\b[^>]*>", " ");
        }
        // 清除残留的单标签和安全检测标签
        text = text.replaceAll("(?is)<(thinking|think|thought|reasoning|answer|output|response|text|safe|unsafe|sensitive)\\b[^>]*/?>", " ");
        // 2. 去除 markdown 代码块
        text = text.replaceAll("```json\\s*", "");
        text = text.replaceAll("```\\s*", "");
        text = text.replaceAll("\\n\\s*", " ");
        text = text.trim();

        // 3. 遍历文本，找到所有可能的 JSON 对象，挨个尝试解析
        List<String> candidates = new ArrayList<>();
        int lastStart = 0;
        while (true) {
            int braceStart = text.indexOf("{", lastStart);
            if (braceStart < 0) break;
            // 从这个 { 往后找闭合的 }，跳过字符串内的 { }
            int depth = 0;
            int i = braceStart;
            boolean found = false;
            while (i < text.length()) {
                char c = text.charAt(i);
                if (c == '"') {
                    i++;
                    while (i < text.length() && text.charAt(i) != '"') {
                        if (text.charAt(i) == '\\') i++;
                        i++;
                    }
                    // 跳过字符串结尾的中文引号（如 LLM 用「」代替英文引号导致的粘连）
                    if (i < text.length() && text.charAt(i) == '」') {
                        i++;
                    }
                } else if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        candidates.add(text.substring(braceStart, i + 1));
                        lastStart = i + 1;
                        found = true;
                        break;
                    }
                }
                i++;
            }
            if (!found) break;
        }

        // 4. 挨个尝试解析，找到包含 "titles" 数组且有有效内容的 JSON
        for (String candidate : candidates) {
            try {
                JsonNode root = objectMapper.readTree(candidate);
                JsonNode titles = root.path("titles");
                if (titles.isArray() && titles.size() > 0) {
                    // 验证至少有一条记录有非空 title
                    boolean hasValid = false;
                    for (JsonNode t : titles) {
                        if (!t.path("title").asText("").trim().isEmpty()) {
                            hasValid = true;
                            break;
                        }
                    }
                    if (hasValid) {
                        log.info("[TitleGenerateScheduler] JSON解析成功, 从 {} 个候选中找到 {} 条", candidates.size(), titles.size());
                        return titles;
                    }
                }
            } catch (Exception ignored) {}
        }

        // 5. 回退：正则提取单个 title 对象
        JsonNode fallback = tryExtractTitlesWithRegex(text);
        if (fallback != null && fallback.size() > 0) {
            log.info("[TitleGenerateScheduler] 正则回退提取到 {} 条标题", fallback.size());
            return fallback;
        }
        return null;
    }

    private JsonNode tryExtractTitlesWithRegex(String text) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "\\{\\s*\"track\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"title\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"description\"\\s*:\\s*\"([^\"]*)\"\\s*\\}");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            com.fasterxml.jackson.databind.node.ArrayNode result = objectMapper.createArrayNode();
            while (matcher.find()) {
                String title = matcher.group(2).trim();
                if (title.isEmpty()) continue;
                com.fasterxml.jackson.databind.node.ObjectNode obj = objectMapper.createObjectNode();
                obj.put("track", matcher.group(1).trim());
                obj.put("title", title);
                obj.put("description", matcher.group(3).trim());
                result.add(obj);
            }
            return result.size() > 0 ? result : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将标题文本分词，返回关键词集合（用于相似度检测）
     */
    private Set<String> tokenize(String text) {
        if (text == null || text.isEmpty()) return Collections.emptySet();
        // 去掉标点和空格，按 2-6 字词分块
        String clean = text.replaceAll("[\\s\\p{Punct}]", "");
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i <= clean.length() - 2; i++) {
            for (int len = 2; len <= Math.min(6, clean.length() - i); len++) {
                tokens.add(clean.substring(i, i + len));
            }
        }
        return tokens;
    }

    /**
     * 计算两个标题的相似度（基于 n-gram 重叠）
     */
    private double similarity(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0;
        Set<String> tokensA = tokenize(a);
        Set<String> tokensB = tokenize(b);
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0;
        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);
        double overlap = (2.0 * intersection.size()) / (tokensA.size() + tokensB.size());
        return overlap;
    }

    private SaveResult saveTitles(List<Map<String, String>> allRows, Map<String, String> trackNameToIdMap, String taskId) {
        int savedCount = 0;
        int skipCount = 0;
        int similarCount = 0;
        Set<String> batchDedupSet = new HashSet<>();
        // 已有关键词映射：keyword -> title（用于相似度检测）
        Map<String, String> existingKeywordTitleMap = new HashMap<>();
        List<TitleLibrary> existingTitles = titleLibraryService.list();
        if (existingTitles != null) {
            for (TitleLibrary et : existingTitles) {
                if (et.getTitle() != null && et.getTitleKeyword() != null && !et.getTitleKeyword().isEmpty()) {
                    existingKeywordTitleMap.put(et.getTitleKeyword(), et.getTitle());
                } else if (et.getTitle() != null) {
                    // 兼容旧数据：从 title 直接生成关键词
                    String kw = String.join(",", tokenize(et.getTitle()));
                    existingKeywordTitleMap.put(kw, et.getTitle());
                }
            }
        }
        // 当前批次内的关键词集合（用于批次内相似度检测）
        Set<String> batchKeywords = new HashSet<>();

        for (Map<String, String> row : allRows) {
            try {
                String trackName = row.get("track");
                String trackId = trackNameToIdMap.get(trackName);
                if (trackId == null && trackName != null && !trackName.isEmpty()) {
                    Track t = trackMapper.findByName(trackName);
                    if (t != null) {
                        trackId = t.getId();
                    }
                }
                String title = row.get("title");
                String platform = row.get("platform");
                String description = row.get("description");

                // 批次内精确去重
                if (!batchDedupSet.add(title)) {
                    skipCount++;
                    continue;
                }

                // 与数据库已有标题精确匹配
                boolean exists = existingTitles != null && existingTitles.stream()
                        .anyMatch(et -> title.equals(et.getTitle()));
                if (exists) {
                    skipCount++;
                    continue;
                }

                // 与数据库已有标题做相似度检测（超过 80% 跳过）
                Set<String> newTokens = tokenize(title);
                String newKw = String.join(",", newTokens);
                boolean tooSimilar = false;
                for (Map.Entry<String, String> existing : existingKeywordTitleMap.entrySet()) {
                    String existingKw = existing.getKey();
                    String existingTitle = existing.getValue();
                    Set<String> existTokens = new HashSet<>(Arrays.asList(existingKw.split(",")));
                    if (existTokens.isEmpty()) continue;
                    Set<String> intersection = new HashSet<>(newTokens);
                    intersection.retainAll(existTokens);
                    double sim = (2.0 * intersection.size()) / (newTokens.size() + existTokens.size());
                    if (sim > 0.8) {
                        log.info("[TitleGenerateScheduler] 标题与已有标题相似度超80%跳过: new='{}' vs existing='{}' (sim={})", title, existingTitle, String.format("%.2f", sim));
                        tooSimilar = true;
                        similarCount++;
                        break;
                    }
                }
                if (tooSimilar) continue;

                // 批次内相似度检测
                for (String existingKw : batchKeywords) {
                    Set<String> existTokens = new HashSet<>(Arrays.asList(existingKw.split(",")));
                    if (existTokens.isEmpty()) continue;
                    Set<String> intersection = new HashSet<>(newTokens);
                    intersection.retainAll(existTokens);
                    double sim = (2.0 * intersection.size()) / (newTokens.size() + existTokens.size());
                    if (sim > 0.8) {
                        log.info("[TitleGenerateScheduler] 批次内标题相似度超80%跳过: '{}' vs '{}' (sim={})", title, title, String.format("%.2f", sim));
                        similarCount++;
                        break;
                    }
                }
                if (tooSimilar) continue;

                batchKeywords.add(newKw);

                TitleLibrary tl = new TitleLibrary();
                tl.setTitle(title);
                tl.setDescription(description);
                tl.setPlatform(platform);
                tl.setTrackId(trackId);
                tl.setTaskId(taskId);
                tl.setTitleKeyword(newKw);
                tl.setUseCount(0);
                titleLibraryService.save(tl);
                // 更新本地已有映射，避免同批次重复入库
                existingKeywordTitleMap.put(newKw, title);
                try {
                    titleReviewService.createReviewRecord(tl.getId(), "ai_generated_v2");
                } catch (Exception e) {
                    log.error("[TitleGenerateScheduler] 创建审核记录失败 title={}: {}", tl.getTitle(), e.getMessage());
                }
                // 违禁词/敏感词检测
                try {
                    String textToCheck = title + (description != null ? " " + description : "");
                    ContentCheckService.CheckResult checkResult = contentCheckService.checkContent(textToCheck);
                    if (checkResult.getTotalChars() > 0 && !checkResult.getMatches().isEmpty()) {
                        String checkJson = objectMapper.writeValueAsString(checkResult);
                        titleLibraryService.updateBannedWordCheckResult(tl.getId(), checkJson);
                    }
                } catch (Exception e) {
                    log.warn("[TitleGenerateScheduler] 标题违禁词检测失败，不影响入库: {}", e.getMessage());
                }
                savedCount++;
            } catch (Exception e) {
                log.error("[TitleGenerateScheduler] 单条入库失败 title={}: {}", row.get("title"), e.getMessage());
            }
        }
        log.info("[TitleGenerateScheduler] 入库统计: 保存={}, 精确重复跳过={}, 相似跳过={}", savedCount, skipCount, similarCount);
        return new SaveResult(savedCount, skipCount + similarCount);
    }

    private void writeExcel(List<Map<String, String>> allRows, String filePath) throws Exception {
        try (Workbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(filePath)) {
            Sheet sheet = wb.createSheet("生成标题");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("标题");
            header.createCell(1).setCellValue("平台");
            header.createCell(2).setCellValue("赛道名称");
            header.createCell(3).setCellValue("写作思路");

            for (int i = 0; i < allRows.size(); i++) {
                Map<String, String> row = allRows.get(i);
                Row r = sheet.createRow(i + 1);
                r.createCell(0).setCellValue(row.get("title"));
                r.createCell(1).setCellValue(row.get("platform"));
                r.createCell(2).setCellValue(row.get("track"));
                r.createCell(3).setCellValue(row.get("description"));
            }
            for (int i = 0; i < 4; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }
            wb.write(fos);
        }
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            List<String> list = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode item : node) {
                    list.add(item.asText());
                }
            }
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private boolean isSocialTrack(String trackName) {
        if (trackName == null) return false;
        String lower = trackName.toLowerCase();
        return lower.contains("社会") || lower.contains("民生") || lower.contains("热点") || lower.contains("时政") || lower.contains("新闻");
    }
}