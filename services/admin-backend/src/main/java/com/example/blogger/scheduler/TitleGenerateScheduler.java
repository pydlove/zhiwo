package com.example.blogger.scheduler;

import com.example.blogger.entity.TitleGenerateTask;
import com.example.blogger.entity.TitleLibrary;
import com.example.blogger.entity.Track;
import com.example.blogger.mapper.TitleGenerateTaskMapper;
import com.example.blogger.mapper.TrackMapper;
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
                                  TrackMapper trackMapper) {
        this.taskMapper = taskMapper;
        this.taskService = taskService;
        this.llmService = llmService;
        this.titleLibraryService = titleLibraryService;
        this.titleReviewService = titleReviewService;
        this.trackMapper = trackMapper;
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

                    String prompt = buildPrompt(platform, batchTracks, countPerCombo, instruction);
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

    private String buildPrompt(String platform, List<Track> batchTracks, int countPerCombo, String instruction) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为\"").append(platform).append("\"平台生成爆款标题。\n\n");
        prompt.append("需要生成标题的赛道：\n");
        boolean hasSocialTrack = false;
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
        }
        prompt.append("\n每个赛道生成").append(countPerCombo).append("个标题。要求：\n");
        prompt.append("1. 标题是爆款风格，吸引眼球，适合").append(platform).append("传播\n");
        if (hasSocialTrack) {
            prompt.append("   【重要】对于社会民生类赛道（如涉及社会、民生、热点、时政、新闻等），标题必须基于本年度（").append(java.time.Year.now().getValue()).append("年）真实发生的事件或话题，严禁虚构、编造不存在的事件或数据。\n");
        }
        prompt.append("2. 每个标题的 track 字段必须是上面给定的赛道名称（纯名称，不要包含括号内的说明），严禁自创赛道名称\n");
        prompt.append("3. 每个标题必须配一段SEO描述（30-50字），要求：\n");
        prompt.append("   - 包含赛道核心关键词，便于搜索引擎收录\n");
        prompt.append("   - 突出文章价值点和读者收益\n");
        prompt.append("   - 语言自然流畅，符合").append(platform).append("的搜索推荐算法偏好\n");
        prompt.append("   - 适当使用数字、疑问、对比等提升点击率的手法\n");
        prompt.append("4. 所有生成的标题必须全局唯一，同一批次内不同赛道之间不得出现相同或高度相似的标题\n");
        prompt.append("5. 标题和描述中禁止出现英文双引号 \"，如有引用需求请使用中文引号「」或『』代替\n");
        prompt.append("6. 只输出纯JSON，不要markdown代码块，不要任何额外文字\n\n");
        prompt.append("格式：{\"titles\":[{\"track\":\"赛道名称\",\"title\":\"标题文字\",\"description\":\"SEO描述\"},...]}");
        if (instruction != null && !instruction.trim().isEmpty()) {
            prompt.append("\n\n【标题生成方向】").append(instruction.trim()).append("（请在生成标题时严格遵循此要求）");
        }
        return prompt.toString();
    }

    private JsonNode extractJsonArray(String llmResponse) {
        String text = llmResponse;
        // 1. 去掉 <think>...</think> 思维链
        text = text.replaceAll("(?s)<think>.*?</think>", "");
        // 2. 尝试去掉 markdown 代码块标记
        if (text.contains("```json")) {
            text = text.substring(text.indexOf("```json") + 7);
            if (text.contains("```")) {
                text = text.substring(0, text.indexOf("```"));
            }
        } else if (text.contains("```")) {
            text = text.substring(text.indexOf("```") + 3);
            if (text.contains("```")) {
                text = text.substring(0, text.indexOf("```"));
            }
        }
        text = text.trim();
        // 3. 找到 JSON 对象开始/结束位置
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        try {
            JsonNode root = objectMapper.readTree(text);
            JsonNode titles = root.path("titles");
            if (titles.isArray() && titles.size() > 0) {
                return titles;
            }
            return titles;
        } catch (Exception e) {
            log.warn("[TitleGenerateScheduler] JSON解析失败: {}", e.getMessage());
            // 4. 回退：用正则提取单个 title 对象
            JsonNode fallback = tryExtractTitlesWithRegex(text);
            if (fallback != null) {
                log.info("[TitleGenerateScheduler] 正则回退提取到 {} 条标题", fallback.size());
                return fallback;
            }
            log.warn("[TitleGenerateScheduler] 回退提取也未找到有效标题，清洗后文本前300字: {}",
                    text.length() > 300 ? text.substring(0, 300) + "..." : text);
            return null;
        }
    }

    private JsonNode tryExtractTitlesWithRegex(String text) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "\\{\\s*\"track\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"title\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"description\"\\s*:\\s*\"([^\"]*)\"\\s*\\}");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            com.fasterxml.jackson.databind.node.ArrayNode result = objectMapper.createArrayNode();
            while (matcher.find()) {
                com.fasterxml.jackson.databind.node.ObjectNode obj = objectMapper.createObjectNode();
                obj.put("track", matcher.group(1));
                obj.put("title", matcher.group(2));
                obj.put("description", matcher.group(3));
                result.add(obj);
            }
            return result.size() > 0 ? result : null;
        } catch (Exception e) {
            return null;
        }
    }

    private SaveResult saveTitles(List<Map<String, String>> allRows, Map<String, String> trackNameToIdMap, String taskId) {
        int savedCount = 0;
        int skipCount = 0;
        Set<String> batchDedupSet = new HashSet<>();
        Set<String> existingSet = new HashSet<>();
        List<TitleLibrary> existingTitles = titleLibraryService.list();
        if (existingTitles != null) {
            for (TitleLibrary et : existingTitles) {
                if (et.getTitle() != null) {
                    existingSet.add(et.getTitle());
                }
            }
        }

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

                if (!batchDedupSet.add(title)) {
                    skipCount++;
                    continue;
                }
                if (existingSet.contains(title)) {
                    skipCount++;
                    continue;
                }

                TitleLibrary tl = new TitleLibrary();
                tl.setTitle(title);
                tl.setDescription(row.get("description"));
                tl.setPlatform(platform);
                tl.setTrackId(trackId);
                tl.setTaskId(taskId);
                tl.setUseCount(0);
                titleLibraryService.save(tl);
                try {
                    titleReviewService.createReviewRecord(tl.getId(), "ai_generated_v2");
                } catch (Exception e) {
                    log.error("[TitleGenerateScheduler] 创建审核记录失败 title={}: {}", tl.getTitle(), e.getMessage());
                }
                savedCount++;
            } catch (Exception e) {
                log.error("[TitleGenerateScheduler] 单条入库失败 title={}: {}", row.get("title"), e.getMessage());
            }
        }
        log.info("[TitleGenerateScheduler] 入库统计: 保存={}, 跳过重复={}", savedCount, skipCount);
        return new SaveResult(savedCount, skipCount);
    }

    private void writeExcel(List<Map<String, String>> allRows, String filePath) throws Exception {
        try (Workbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(filePath)) {
            Sheet sheet = wb.createSheet("生成标题");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("标题");
            header.createCell(1).setCellValue("平台");
            header.createCell(2).setCellValue("赛道名称");
            header.createCell(3).setCellValue("描述");

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
