package com.example.blogger.util;

import java.util.*;

/**
 * 文章 AI 检测工具
 * 基于统计特征的启发式检测（无需外部 AI 服务）
 *
 * 检测维度：
 * 1. 文本熵（字符多样性）
 * 2. 句子长度方差（burstiness）
 * 3. 段落重复率
 * 4. 字符 n-gram 自然度
 * 5. 特定 AI 写作模式识别
 */
public class TextAiDetectUtil {

    /**
     * 检测文本，返回 AI 概率（0-100）
     */
    public static AiDetectResult detect(String text) {
        if (text == null || text.trim().length() < 50) {
            return new AiDetectResult(0, Collections.emptyList(), "文本太短，无法准确检测");
        }
        text = text.trim();
        List<String> reasons = new ArrayList<>();
        int score = 0;

        // 1. 字符熵检测
        int entropyScore = checkEntropy(text);
        if (entropyScore > 0) {
            score += entropyScore;
            reasons.add("字符多样性异常");
        }

        // 2. 句子长度方差检测（AI 文章方差通常偏小）
        int burstinessScore = checkBurstiness(text);
        if (burstinessScore > 0) {
            score += burstinessScore;
            reasons.add("句子长度过于均匀");
        }

        // 3. 段落重复检测
        int repeatScore = checkRepetition(text);
        if (repeatScore > 0) {
            score += repeatScore;
            reasons.add("存在段落重复模式");
        }

        // 4. AI 特征词检测
        int aiWordScore = checkAiPatterns(text);
        if (aiWordScore > 0) {
            score += aiWordScore;
            reasons.add("包含 AI 典型表述");
        }

        // 5. 标点符号分布检测
        int punctScore = checkPunctuation(text);
        if (punctScore > 0) {
            score += punctScore;
            reasons.add("标点使用模式异常");
        }

        // 6. 首句开头模式检测（AI 常用固定开头）
        int openingScore = checkOpenings(text);
        if (openingScore > 0) {
            score += openingScore;
            reasons.add("文章开头符合 AI 常见模式");
        }

        // cap at 100
        score = Math.min(100, Math.max(0, score));

        String level;
        if (score >= 70) level = "高风险";
        else if (score >= 40) level = "中风险";
        else level = "低风险";

        return new AiDetectResult(score, reasons, level);
    }

    private static int checkEntropy(String text) {
        // 计算字符信息熵
        Map<Character, Integer> freq = new HashMap<>();
        int total = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)) {
                freq.merge(c, 1, Integer::sum);
                total++;
            }
        }
        if (total < 100) return 0;

        double entropy = 0;
        for (int f : freq.values()) {
            double p = (double) f / total;
            entropy -= p * (Math.log(p) / Math.log(2));
        }

        // AI 文本字符熵通常偏高或偏低（过于均匀或刻意多样）
        // 正常中文文本熵约 8-10 bits
        if (entropy < 6.5 || entropy > 11.5) {
            return 15;
        }
        if (entropy < 7.5 || entropy > 10.5) {
            return 8;
        }
        return 0;
    }

    private static int checkBurstiness(String text) {
        // 计算句子长度标准差 / 均值（burstiness ratio）
        String[] sentences = text.split("[。！？.!?\n]+");
        if (sentences.length < 5) return 0;

        List<Integer> lengths = new ArrayList<>();
        for (String s : sentences) {
            int len = s.trim().length();
            if (len > 5) lengths.add(len);
        }
        if (lengths.size() < 5) return 0;

        double mean = lengths.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = lengths.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);

        // burstiness = stdDev / mean（如果太小，说明句子长度过于一致）
        double burstiness = (mean > 0) ? stdDev / mean : 0;

        // AI 文本 burstiness 通常较低（句子长度过于一致）
        if (burstiness < 0.25) {
            return 20;
        }
        if (burstiness < 0.40) {
            return 10;
        }
        return 0;
    }

    private static int checkRepetition(String text) {
        // 简化重复检测：找连续 5-gram 重复
        String[] paragraphs = text.split("\n");
        if (paragraphs.length < 2) return 0;

        int repeatCount = 0;
        Set<String> seenBigrams = new HashSet<>();
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.length() < 20) continue;
            for (int i = 0; i < trimmed.length() - 4; i++) {
                String bigram = trimmed.substring(i, i + 5).toLowerCase();
                if (seenBigrams.contains(bigram)) {
                    repeatCount++;
                }
                seenBigrams.add(bigram);
            }
        }

        // 超过阈值判定为有重复模式
        if (repeatCount > 8) {
            return 15;
        }
        if (repeatCount > 3) {
            return 8;
        }
        return 0;
    }

    private static int checkAiPatterns(String text) {
        int score = 0;
        String lower = text.toLowerCase();

        // AI 常使用的套路句式（每个命中+4分，最多20）
        String[] aiOpenings = {
            "值得注意的是", "值得注意的是，",
            "在当今社会", "在当今时代", "随着科技的发展", "随着社会的进步",
            "不得不说的是", "客观来说", "总的来说", "总体而言",
            "从多个角度来看", "综上所述", "从这个角度来看", "从某种程度上说",
            "从这个意义上说",
            "我们不难发现", "不难发现", "可以看出", "可以看出，",
            "可以说", "可以说，", "换句话说", "也就是说", "具体来说",
            "毫无疑问", "毋庸置疑", "显而易见", "显而易见的是",
            "首先", "首先，", "其次", "其次，", "最后", "最后，",
            "一方面", "另一方面", "不仅", "而且", "不但", "还",
            "虽然", "但是", "然而", "因此", "所以",
        };

        int openingCount = 0;
        for (String opening : aiOpenings) {
            if (lower.contains(opening)) {
                openingCount++;
            }
        }
        score += Math.min(20, openingCount * 4);

        // AI 常用连接词与过渡短语（每个命中+3分，最多12）
        String[] aiConnectors = {
            "与此同时", "更重要的是", "除此之外", "除此以外",
            "此外", "另外", "再者", "进而", "从而", "因而",
            "无独有偶", "相形见绌", "如出一辙", "大同小异",
            "不言而喻", "显而易见", "毋庸置疑", "毫无疑问",
        };
        int connectorCount = 0;
        for (String c : aiConnectors) {
            if (lower.contains(c)) connectorCount++;
        }
        score += Math.min(12, connectorCount * 3);

        // 过度结构化表达（每个命中+4分，最多8）
        String[] structuredPatterns = {
            "第一，", "第二，", "第三，", "第四，", "第五，",
            "其一", "其二", "其三",
            "一是", "二是", "三是",
        };
        int structCount = 0;
        for (String p : structuredPatterns) {
            if (lower.contains(p)) structCount++;
        }
        score += Math.min(8, structCount * 4);

        return Math.min(25, score);
    }

    private static int checkPunctuation(String text) {
        // 检测标点密度
        String puncts = "\uff0c\uff01\uff1f\uff1b\uff1a\u3001\u201c\u201d\u2018\u2019\uff08\uff09\u300e\u300f";
        long punctCount = text.chars().filter(c -> puncts.indexOf(c) >= 0).count();
        long letterCount = text.chars().filter(c -> Character.isLetter(c)).count();

        if (letterCount < 100) return 0;

        double punctRatio = (double) punctCount / letterCount;

        // 正常中文标点密度约 0.3-0.5，过高或过低都可疑
        if (punctRatio < 0.15 || punctRatio > 0.65) {
            return 12;
        }
        if (punctRatio < 0.25 || punctRatio > 0.55) {
            return 6;
        }
        return 0;
    }

    private static int checkOpenings(String text) {
        // 检测 AI 典型的首句模式
        String[] firstLines = text.split("\n");
        if (firstLines.length == 0) return 0;
        String first = firstLines[0].trim().toLowerCase();

        // 以特定 AI 特征词开头
        String[] aiStarts = {
            "首先，", "值得注意的是", "在当今", "随着",
            "我们都知道", "事实上", "不可否认", "毋庸置疑",
            "显而易见", "总体而言", "总的来说",
        };
        for (String start : aiStarts) {
            if (first.startsWith(start)) {
                return 15;
            }
        }
        return 0;
    }

    public static class AiDetectResult {
        private final int score;              // 0-100
        private final List<String> reasons;   // 判定原因
        private final String level;            // 高/中/低风险

        public AiDetectResult(int score, List<String> reasons, String level) {
            this.score = score;
            this.reasons = reasons;
            this.level = level;
        }

        public int getScore() { return score; }
        public List<String> getReasons() { return reasons; }
        public String getLevel() { return level; }
    }
}
