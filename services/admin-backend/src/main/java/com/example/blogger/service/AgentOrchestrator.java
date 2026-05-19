package com.example.blogger.service;

import com.example.blogger.entity.*;
import com.example.blogger.mapper.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final AgentConfigMapper agentConfigMapper;
    private final AgentExecutionMapper agentExecutionMapper;
    private final AgentTitleMatcher agentTitleMatcher;
    private final TitleLibraryMapper titleLibraryMapper;
    private final TitleLibraryService titleLibraryService;
    private final TitleGenerateTaskService titleGenerateTaskService;
    private final TitleGenerationTaskService titleGenerationTaskService;
    private final UserMapper userMapper;
    private final TrackMapper trackMapper;
    private final TitleRecommendationMapper titleRecommendationMapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public AgentOrchestrator(AgentConfigMapper agentConfigMapper,
                              AgentExecutionMapper agentExecutionMapper,
                              AgentTitleMatcher agentTitleMatcher,
                              TitleLibraryMapper titleLibraryMapper,
                              TitleLibraryService titleLibraryService,
                              TitleGenerateTaskService titleGenerateTaskService,
                              TitleGenerationTaskService titleGenerationTaskService,
                              UserMapper userMapper,
                              TrackMapper trackMapper,
                              TitleRecommendationMapper titleRecommendationMapper) {
        this.agentConfigMapper = agentConfigMapper;
        this.agentExecutionMapper = agentExecutionMapper;
        this.agentTitleMatcher = agentTitleMatcher;
        this.titleLibraryMapper = titleLibraryMapper;
        this.titleLibraryService = titleLibraryService;
        this.titleGenerateTaskService = titleGenerateTaskService;
        this.titleGenerationTaskService = titleGenerationTaskService;
        this.userMapper = userMapper;
        this.trackMapper = trackMapper;
        this.titleRecommendationMapper = titleRecommendationMapper;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 执行一次完整的 Agent 流水线
     */
    public void executeAgentRun() {
        AgentConfig config = agentConfigMapper.findOne();
        if (config == null || !Integer.valueOf(1).equals(config.getEnabled())) {
            log.info("[AgentOrchestrator] Agent 未启用，跳过执行");
            return;
        }

        LocalDate today = LocalDate.now();
        log.info("[AgentOrchestrator] ====== Agent 执行开始: {} ======", today);

        AgentExecution execution = new AgentExecution();
        execution.setExecutionDate(today);
        execution.setStatus("running");
        execution.setStartedAt(LocalDateTime.now());
        agentExecutionMapper.insert(execution);

        int totalMatched = 0;
        int totalGenerated = 0;
        int totalArticleTasks = 0;
        int totalFailed = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        try {
            // 1. 获取所有有效用户
            List<User> users = userMapper.findAll().stream()
                    .filter(u -> u.getIsDeleted() == null || u.getIsDeleted() != 1)
                    .collect(Collectors.toList());

            // 2. 获取所有有效赛道
            List<Track> tracks = trackMapper.findAll().stream()
                    .filter(t -> t.getIsDeleted() == null || t.getIsDeleted() != 1)
                    .collect(Collectors.toList());

            execution.setTotalUsers(users.size());
            execution.setTotalTracks(tracks.size());
            agentExecutionMapper.update(execution);

            log.info("[AgentOrchestrator] 用户={}, 赛道={}", users.size(), tracks.size());

            // 3. 遍历每个 (用户, 赛道) 组合
            for (User user : users) {
                for (Track track : tracks) {
                    try {
                        Map<String, Object> detail = processUserTrack(user, track, config, today);
                        details.add(detail);

                        totalMatched += (int) detail.getOrDefault("matchedCount", 0);
                        totalGenerated += (int) detail.getOrDefault("generatedCount", 0);
                        totalArticleTasks += (int) detail.getOrDefault("articleTasks", 0);

                        if (Boolean.TRUE.equals(detail.get("failed"))) {
                            totalFailed++;
                        }
                    } catch (Exception e) {
                        log.error("[AgentOrchestrator] 处理 user={}, track={} 失败", user.getId(), track.getId(), e);
                        totalFailed++;
                        Map<String, Object> detail = new HashMap<>();
                        detail.put("userId", user.getId());
                        detail.put("trackId", track.getId());
                        detail.put("failed", true);
                        detail.put("error", e.getMessage());
                        details.add(detail);
                    }
                }
            }

            // 4. 更新执行记录
            execution.setStatus(totalFailed > 0 ? "partial" : "completed");
            execution.setMatchedTitles(totalMatched);
            execution.setGeneratedTitles(totalGenerated);
            execution.setArticleTasks(totalArticleTasks);
            execution.setFailedCount(totalFailed);
            execution.setCompletedAt(LocalDateTime.now());
            execution.setDetailJson(objectMapper.writeValueAsString(details));
            agentExecutionMapper.update(execution);

            log.info("[AgentOrchestrator] ====== Agent 执行完成: matched={}, generated={}, articles={}, failed={} ======",
                    totalMatched, totalGenerated, totalArticleTasks, totalFailed);

        } catch (Exception e) {
            log.error("[AgentOrchestrator] Agent 执行异常", e);
            execution.setStatus("failed");
            execution.setCompletedAt(LocalDateTime.now());
            execution.setErrorMessage(e.getMessage());
            try {
                execution.setDetailJson(objectMapper.writeValueAsString(details));
            } catch (Exception ignored) {}
            agentExecutionMapper.update(execution);
        }
    }

    /**
     * 处理单个 (用户, 赛道) 组合
     */
    private Map<String, Object> processUserTrack(User user, Track track, AgentConfig config, LocalDate today) throws Exception {
        String userId = user.getId();
        String trackId = track.getId();
        String trackName = track.getName();

        Map<String, Object> detail = new HashMap<>();
        detail.put("userId", userId);
        detail.put("trackId", trackId);
        detail.put("trackName", trackName);
        detail.put("failed", false);

        log.info("[AgentOrchestrator] 处理 user={}, track={}", userId, trackId);

        // 1. 查询历史标题
        List<TitleLibrary> historyTitles = titleLibraryMapper.findRecentByUserAndTrack(
                userId, trackId, config.getHistoryDays(), 50);

        // 2. 查询候选标题
        List<TitleLibrary> candidates = titleLibraryMapper.findAvailableByTrack(
                trackId, config.getCandidateLimit());

        log.info("[AgentOrchestrator] user={}, track={}, history={}, candidates={}",
                userId, trackId, historyTitles.size(), candidates.size());

        // 3. AI 选标题
        AgentTitleMatcher.MatchResult matchResult = agentTitleMatcher.selectTitles(
                userId, trackId, trackName, historyTitles, candidates, config);

        List<TitleLibrary> selected = matchResult.selected;

        // 4. 如果需要生成标题
        if (matchResult.needGenerate && selected.size() < config.getMinTitlesPerTrack()) {
            log.info("[AgentOrchestrator] 需要生成新标题: user={}, track={}, reason={}",
                    userId, trackId, matchResult.reason);

            // 创建标题生成任务
            String platforms = track.getPlatforms() != null ? track.getPlatforms() : "公众号";
            String trackIdsJson = "[\"" + trackId + "\"]";
            TitleGenerateTask genTask = titleGenerateTaskService.createTask(
                    platforms, trackIdsJson, config.getMinTitlesPerTrack() * 2,
                    "AI Agent 自动补充标题：" + trackName, null);

            log.info("[AgentOrchestrator] 标题生成任务创建: taskId={}", genTask.getId());

            // 轮询等待完成（最多 10 分钟）
            boolean finished = waitForTitleGeneration(genTask.getId(), 120); // 120 * 5s = 10min

            if (finished) {
                // 重新获取候选标题
                candidates = titleLibraryMapper.findAvailableByTrack(trackId, config.getCandidateLimit());
                matchResult = agentTitleMatcher.selectTitles(
                        userId, trackId, trackName, historyTitles, candidates, config);
                selected = matchResult.selected;
                detail.put("generatedCount", matchResult.needGenerate ? config.getMinTitlesPerTrack() : 0);
            } else {
                log.warn("[AgentOrchestrator] 标题生成超时: taskId={}", genTask.getId());
                detail.put("generatedCount", 0);
                detail.put("error", "标题生成超时");
            }
        }

        // 5. 关联用户（创建 TitleRecommendation）
        int matchedCount = 0;
        for (TitleLibrary title : selected) {
            try {
                // 检查是否已关联
                int existing = titleRecommendationMapper.countByTitleAndDate(title.getId(), today);
                if (existing > 0) {
                    log.debug("[AgentOrchestrator] 标题已关联，跳过: titleId={}", title.getId());
                    continue;
                }

                TitleRecommendation rec = new TitleRecommendation();
                rec.setTitleLibraryId(title.getId());
                rec.setUserId(userId);
                rec.setPlatform(title.getPlatform());
                rec.setTrackId(trackId);
                rec.setRecommendDate(today);
                titleRecommendationMapper.insert(rec);
                matchedCount++;

                // 标记标题为已使用
                titleLibraryService.updateIsUsed(title.getId(), 1);
            } catch (Exception e) {
                log.error("[AgentOrchestrator] 关联标题失败: titleId={}", title.getId(), e);
            }
        }

        detail.put("matchedCount", matchedCount);
        log.info("[AgentOrchestrator] 关联完成: user={}, track={}, matched={}",
                userId, trackId, matchedCount);

        // 6. 创建文章生成任务
        int articleTasks = 0;
        for (TitleLibrary title : selected) {
            try {
                // 检查是否已有 pending/processing 任务
                List<TitleGenerationTask> existingTasks = titleGenerationTaskService.findByTitleLibraryId(title.getId());
                boolean hasPending = existingTasks.stream()
                        .anyMatch(t -> "pending".equals(t.getStatus()) || "processing".equals(t.getStatus()));
                if (hasPending) {
                    log.debug("[AgentOrchestrator] 已有待处理任务，跳过: titleId={}", title.getId());
                    continue;
                }

                // 构建 prompt（复用手动流程逻辑）
                String prompt = buildArticlePrompt(title);
                TitleGenerationTask task = titleGenerationTaskService.createTask(
                        title.getId(), title.getTitle(), prompt);
                articleTasks++;
                log.debug("[AgentOrchestrator] 文章任务创建: taskId={}, titleId={}", task.getId(), title.getId());
            } catch (Exception e) {
                log.error("[AgentOrchestrator] 创建文章任务失败: titleId={}", title.getId(), e);
            }
        }

        detail.put("articleTasks", articleTasks);
        log.info("[AgentOrchestrator] 文章任务创建完成: user={}, track={}, tasks={}",
                userId, trackId, articleTasks);

        return detail;
    }

    /**
     * 轮询等待标题生成任务完成
     */
    private boolean waitForTitleGeneration(String taskId, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                Thread.sleep(5000); // 每 5 秒检查一次
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }

            TitleGenerateTask task = titleGenerateTaskService.findById(taskId);
            if (task == null) return false;

            if ("completed".equals(task.getStatus())) {
                log.info("[AgentOrchestrator] 标题生成完成: taskId={}", taskId);
                return true;
            }
            if ("failed".equals(task.getStatus()) || "stopped".equals(task.getStatus())) {
                log.warn("[AgentOrchestrator] 标题生成失败/停止: taskId={}, status={}", taskId, task.getStatus());
                return false;
            }
        }
        return false;
    }

    /**
     * 构建文章生成 prompt（简化版，复用手动流程核心逻辑）
     */
    private String buildArticlePrompt(TitleLibrary titleLib) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位资深新媒体撰稿人，请根据以下标题创作一篇高质量公众号文章。\n\n");
        prompt.append("标题：").append(titleLib.getTitle()).append("\n");
        if (titleLib.getDescription() != null && !titleLib.getDescription().isEmpty()) {
            prompt.append("描述：").append(titleLib.getDescription()).append("\n");
        }
        prompt.append("\n要求：\n");
        prompt.append("1. 文章结构清晰，有小标题分段\n");
        prompt.append("2. 语言生动有感染力，避免机械式表达\n");
        prompt.append("3. 适当使用故事、案例、数据增强说服力\n");
        prompt.append("4. 结尾要有总结或引导互动\n");
        prompt.append("5. 总字数控制在 1500-2500 字\n");
        prompt.append("\n请直接输出文章正文，不需要额外解释。");
        return prompt.toString();
    }
}
