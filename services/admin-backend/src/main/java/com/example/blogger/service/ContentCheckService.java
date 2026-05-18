package com.example.blogger.service;

import com.example.blogger.entity.BannedWord;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ContentCheckService {

    private final BannedWordService bannedWordService;

    public ContentCheckService(BannedWordService bannedWordService) {
        this.bannedWordService = bannedWordService;
    }

    /**
     * 检测内容中的违禁词和敏感词（只检测，不替换）
     */
    public CheckResult checkContent(String content) {
        if (content == null || content.isEmpty()) {
            return new CheckResult(0, new HashMap<>(), new ArrayList<>());
        }

        List<BannedWord> allWords = bannedWordService.list();
        if (allWords == null || allWords.isEmpty()) {
            int totalChars = content.replaceAll("\\s+", "").length();
            return new CheckResult(totalChars, new HashMap<>(), new ArrayList<>());
        }

        List<WordMatch> matches = new ArrayList<>();
        Map<String, Integer> categoryCounts = new HashMap<>();

        // 初始化所有分类计数为0
        Set<String> allCategories = new HashSet<>(Arrays.asList("极限词", "医疗词", "金融词", "诱导词", "政治敏感", "敏感词", "其他"));
        for (String cat : allCategories) {
            categoryCounts.put(cat, 0);
        }

        // 按词长度降序排序，优先匹配长词（避免短词干扰）
        List<BannedWord> sortedWords = new ArrayList<>(allWords);
        sortedWords.sort((a, b) -> Integer.compare(b.getWord().length(), a.getWord().length()));

        // 记录已匹配的区间 [start, end)，避免重叠重复计数
        List<int[]> matchedRanges = new ArrayList<>();

        for (BannedWord bw : sortedWords) {
            String word = bw.getWord();
            if (word == null || word.isEmpty()) continue;

            String escaped = Pattern.quote(word);
            Pattern pattern = Pattern.compile(escaped);
            Matcher matcher = pattern.matcher(content);

            int count = 0;
            List<Integer> positions = new ArrayList<>();
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();

                // 检查是否与已匹配区间重叠
                boolean overlaps = false;
                for (int[] range : matchedRanges) {
                    if (start < range[1] && end > range[0]) {
                        overlaps = true;
                        break;
                    }
                }
                if (overlaps) continue;

                matchedRanges.add(new int[]{start, end});
                count++;
                positions.add(start);
            }

            if (count > 0) {
                String category = bw.getCategory() != null ? bw.getCategory() : "其他";
                categoryCounts.merge(category, count, Integer::sum);
                WordMatch match = new WordMatch();
                match.setWord(word);
                match.setCategory(category);
                match.setSeverity(bw.getSeverity());
                match.setCount(count);
                match.setPositions(positions);
                matches.add(match);
            }
        }

        int totalChars = content.replaceAll("\\s+", "").length();
        return new CheckResult(totalChars, categoryCounts, matches);
    }

    public static class CheckResult {
        private int totalChars;
        private Map<String, Integer> categoryCounts;
        private List<WordMatch> matches;

        public CheckResult(int totalChars, Map<String, Integer> categoryCounts, List<WordMatch> matches) {
            this.totalChars = totalChars;
            this.categoryCounts = categoryCounts;
            this.matches = matches;
        }

        public int getTotalChars() { return totalChars; }
        public void setTotalChars(int totalChars) { this.totalChars = totalChars; }
        public Map<String, Integer> getCategoryCounts() { return categoryCounts; }
        public void setCategoryCounts(Map<String, Integer> categoryCounts) { this.categoryCounts = categoryCounts; }
        public List<WordMatch> getMatches() { return matches; }
        public void setMatches(List<WordMatch> matches) { this.matches = matches; }
    }

    public static class WordMatch {
        private String word;
        private String category;
        private String severity;
        private int count;
        private List<Integer> positions;

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public List<Integer> getPositions() { return positions; }
        public void setPositions(List<Integer> positions) { this.positions = positions; }
    }
}
