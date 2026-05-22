package com.example.blogger.service;

import com.example.blogger.entity.AgentConfig;
import com.example.blogger.entity.TitleLibrary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AgentTitleMatcher {

    private static final Logger log = LoggerFactory.getLogger(AgentTitleMatcher.class);
    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    @Autowired
    public AgentTitleMatcher(LLMService llmService) {
        this.llmService = llmService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 标题匹配结果
     */
    public static class MatchResult {
        public List<TitleLibrary> selected = new ArrayList<>();
        public boolean needGenerate = false;
        public String reason = "";

        public MatchResult(List<TitleLibrary> selected, boolean needGenerate, String reason) {
            this.selected = selected != null ? selected : new ArrayList<>();
            this.needGenerate = needGenerate;
            this.reason = reason;
        }
    }

    /**
     * 主入口：为指定用户和赛道智能选标题
     *
     * @param userId       用户ID
     * @param trackId      赛道ID
     * @param trackName    赛道名称
     * @param historyTitles 用户历史推荐标题
     * @param candidates   候选标题
     * @param config       Agent 配置
     * @return 匹配结果
     */
    public MatchResult selectTitles(String userId, String trackId, String trackName,
                                     List<TitleLibrary> historyTitles,
                                     List<TitleLibrary> candidates,
                                     AgentConfig config) {
        int minNeeded = config.getMinTitlesPerTrack();
        double simThreshold = config.getSimilarityThreshold();
        double homoThreshold = config.getHomogeneityThreshold();

        log.info("[AgentTitleMatcher] user={}, track={}, history={}, candidates={}, minNeeded={}",
                userId, trackId, historyTitles.size(), candidates.size(), minNeeded);

        // 1. 程序层粗筛：过滤与历史高相似的候选
        List<TitleLibrary> filtered = filterByHistory(candidates, historyTitles, 0.5);
        log.info("[AgentTitleMatcher] 粗筛后候选数: {}", filtered.size());

        if (filtered.isEmpty()) {
            return new MatchResult(Collections.emptyList(), true, "候选库为空，需生成新标题");
        }

        // 2. 候选内部去重，保留差异最大的 top 30
        filtered = diversifyCandidates(filtered, 30);
        log.info("[AgentTitleMatcher] 去重后候选数: {}", filtered.size());

        // 3. LLM 精筛
        LLMSelection llmSelection = callLLM(filtered, historyTitles, trackName, minNeeded);
        log.info("[AgentTitleMatcher] LLM 选中 {} 条, needGenerate={}",
                llmSelection.selectedIds.size(), llmSelection.needGenerate);

        // 4. 根据 LLM 返回的 ID 找到对应标题对象
        Map<String, TitleLibrary> candidateMap = filtered.stream()
                .collect(Collectors.toMap(TitleLibrary::getId, t -> t));
        List<TitleLibrary> selected = llmSelection.selectedIds.stream()
                .map(candidateMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 5. 程序层阈值校验（兜底）
        List<TitleLibrary> verified = verifyThresholds(selected, historyTitles, simThreshold, homoThreshold);
        log.info("[AgentTitleMatcher] 阈值校验后: {}/{}", verified.size(), selected.size());

        // 6. 如果数量不足，触发补全
        if (verified.size() < minNeeded) {
            return new MatchResult(verified, true,
                    String.format("选中 %d 条，不足 %d 条，需生成新标题", verified.size(), minNeeded));
        }

        return new MatchResult(verified, false, "匹配成功");
    }

    // ==================== Jaccard 相似度 ====================

    /**
     * 字符级 2-gram Jaccard 相似度
     */
    double jaccardSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0;
        String t1 = text1.trim();
        String t2 = text2.trim();
        if (t1.isEmpty() || t2.isEmpty()) return 0;

        Set<String> set1 = ngrams(t1, 2);
        Set<String> set2 = ngrams(t2, 2);

        if (set1.isEmpty() || set2.isEmpty()) return 0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    private Set<String> ngrams(String text, int n) {
        Set<String> result = new HashSet<>();
        for (int i = 0; i <= text.length() - n; i++) {
            result.add(text.substring(i, i + n));
        }
        return result;
    }

    // ==================== 粗筛逻辑 ====================

    /**
     * 过滤与历史标题相似度 > threshold 的候选
     */
    List<TitleLibrary> filterByHistory(List<TitleLibrary> candidates,
                                       List<TitleLibrary> historyTitles,
                                       double threshold) {
        if (historyTitles == null || historyTitles.isEmpty()) {
            return new ArrayList<>(candidates);
        }
        return candidates.stream()
                .filter(c -> {
                    String cTitle = c.getTitle();
                    return historyTitles.stream()
                            .noneMatch(h -> jaccardSimilarity(cTitle, h.getTitle()) > threshold);
                })
                .collect(Collectors.toList());
    }

    /**
     * 候选内部去重，保留差异最大的 topK
     * 贪心策略：每次选与已选集合相似度最小的
     */
    List<TitleLibrary> diversifyCandidates(List<TitleLibrary> candidates, int topK) {
        if (candidates.size() <= topK) return new ArrayList<>(candidates);

        List<TitleLibrary> result = new ArrayList<>();
        List<TitleLibrary> remaining = new ArrayList<>(candidates);

        // 先选第一个
        result.add(remaining.remove(0));

        while (result.size() < topK && !remaining.isEmpty()) {
            TitleLibrary best = null;
            double bestMinSim = Double.MAX_VALUE;

            for (TitleLibrary candidate : remaining) {
                double minSim = Double.MAX_VALUE;
                for (TitleLibrary selected : result) {
                    double sim = jaccardSimilarity(candidate.getTitle(), selected.getTitle());
                    if (sim < minSim) minSim = sim;
                }
                if (minSim < bestMinSim) {
                    bestMinSim = minSim;
                    best = candidate;
                }
            }

            if (best != null) {
                result.add(best);
                remaining.remove(best);
            }
        }

        return result;
    }

    // ==================== LLM 精筛 ====================

    static class LLMSelection {
        List<String> selectedIds = new ArrayList<>();
        boolean needGenerate = false;
    }

    private LLMSelection callLLM(List<TitleLibrary> candidates,
                                  List<TitleLibrary> historyTitles,
                                  String trackName,
                                  int minNeeded) {
        LLMSelection result = new LLMSelection();

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个资深新媒体内容运营专家，擅长从标题库中挑选适合推送的有思想、有创造性的标题。\n\n");
        prompt.append("赛道：").append(trackName != null ? trackName : "未知赛道").append("\n\n");

        // 历史标题（最多50条）
        prompt.append("=== 该用户近期已推荐标题（近30天） ===\n");
        if (historyTitles != null && !historyTitles.isEmpty()) {
            for (int i = 0; i < historyTitles.size(); i++) {
                prompt.append(i + 1).append(". ").append(historyTitles.get(i).getTitle()).append("\n");
            }
        } else {
            prompt.append("（无历史记录）\n");
        }
        prompt.append("\n");

        // 候选标题
        prompt.append("=== 候选标题库（已程序去重） ===\n");
        for (int i = 0; i < candidates.size(); i++) {
            TitleLibrary t = candidates.get(i);
            prompt.append("ID:").append(t.getId()).append(" | ")
                    .append(t.getTitle()).append("\n");
        }
        prompt.append("\n");

        // 指令
        prompt.append("请从候选标题中选出 ").append(minNeeded).append(" 个最合适的标题。\n");
        prompt.append("选择标准：\n");
        prompt.append("1. 与历史标题语义不重复，避免同一用户看到相似内容\n");
        prompt.append("2. 选出的标题之间差异化明显，覆盖不同角度\n");
        prompt.append("3. 符合公众号爆款特征：有悬念、有共鸣、有实用价值\n");
        prompt.append("4. 优先选择未使用过的（候选标题都是未使用的）\n\n");
        prompt.append("如果候选标题整体质量差、同质化严重、或数量不足，请设置 need_generate=true。\n\n");
        prompt.append("请严格按以下 JSON 格式输出，不要有多余解释：\n");
        prompt.append("{\n");
        prompt.append("  \"selected\": [\"id1\", \"id2\", ...],\n");
        prompt.append("  \"need_generate\": false,\n");
        prompt.append("  \"reason\": \"选中理由或需生成的原因\"\n");
        prompt.append("}");

        try {
            String response = llmService.generateContent(prompt.toString());
            log.info("[AgentTitleMatcher] LLM 返回长度={}", response.length());

            // 清理 think 标签
            String cleaned = response.replaceAll("<think>.*?</think>", "").trim();
            if (cleaned.isEmpty()) cleaned = response;

            // 尝试提取 JSON
            JsonNode root = extractJson(cleaned);
            if (root == null) {
                log.warn("[AgentTitleMatcher] 无法解析 LLM 返回的 JSON");
                result.needGenerate = true;
                return result;
            }

            if (root.has("selected") && root.get("selected").isArray()) {
                JsonNode arr = root.get("selected");
                for (JsonNode node : arr) {
                    result.selectedIds.add(node.asText());
                }
            }
            if (root.has("need_generate")) {
                result.needGenerate = root.get("need_generate").asBoolean();
            }
            log.info("[AgentTitleMatcher] LLM 解析结果: selected={}, needGenerate={}",
                    result.selectedIds.size(), result.needGenerate);
        } catch (Exception e) {
            log.error("[AgentTitleMatcher] LLM 调用失败", e);
            result.needGenerate = true;
        }

        return result;
    }

    private JsonNode extractJson(String text) {
        // 策略1：找最外层大括号
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            try {
                return objectMapper.readTree(text.substring(start, end + 1));
            } catch (Exception ignored) {}
        }
        // 策略2：整个文本尝试
        try {
            return objectMapper.readTree(text);
        } catch (Exception ignored) {}
        return null;
    }

    // ==================== 阈值校验 ====================

    /**
     * 程序层阈值校验：逐条检查相似度和同质化
     * 不通过的标题从选中列表移除
     */
    List<TitleLibrary> verifyThresholds(List<TitleLibrary> selected,
                                        List<TitleLibrary> historyTitles,
                                        double simThreshold,
                                        double homoThreshold) {
        if (selected == null || selected.isEmpty()) return Collections.emptyList();

        List<TitleLibrary> verified = new ArrayList<>();

        for (TitleLibrary candidate : selected) {
            String cTitle = candidate.getTitle();
            boolean passSimilarity = true;

            // 1. 相似度校验：与历史标题 pairwise
            if (historyTitles != null) {
                for (TitleLibrary h : historyTitles) {
                    if (jaccardSimilarity(cTitle, h.getTitle()) > simThreshold) {
                        passSimilarity = false;
                        log.debug("[AgentTitleMatcher] 相似度不通过: {} vs {}", cTitle, h.getTitle());
                        break;
                    }
                }
            }

            if (!passSimilarity) continue;

            // 2. 同质化校验：与已验证标题的平均相似度
            if (!verified.isEmpty()) {
                double avgSim = 0;
                for (TitleLibrary v : verified) {
                    avgSim += jaccardSimilarity(cTitle, v.getTitle());
                }
                avgSim /= verified.size();
                if (avgSim > homoThreshold) {
                    log.debug("[AgentTitleMatcher] 同质化不通过: {} avgSim={}", cTitle, avgSim);
                    continue;
                }
            }

            verified.add(candidate);
        }

        return verified;
    }
}
