package com.example.blogger.util;

import java.util.*;

public class TextSimilarityUtil {

    /**
     * 计算两个文本的相似度（基于字符 bigram 的余弦相似度）
     * 返回 0.0 ~ 1.0，值越大越相似
     */
    public static double similarity(String s1, String s2) {
        if (s1 == null) s1 = "";
        if (s2 == null) s2 = "";
        s1 = normalize(s1);
        s2 = normalize(s2);
        if (s1.isEmpty() && s2.isEmpty()) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        Map<String, Integer> freq1 = bigramFreq(s1);
        Map<String, Integer> freq2 = bigramFreq(s2);

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(freq1.keySet());
        allKeys.addAll(freq2.keySet());

        double dotProduct = 0;
        for (String key : allKeys) {
            dotProduct += freq1.getOrDefault(key, 0) * freq2.getOrDefault(key, 0);
        }
        double norm1 = Math.sqrt(sumSquares(freq1.values()));
        double norm2 = Math.sqrt(sumSquares(freq2.values()));
        if (norm1 == 0 || norm2 == 0) return 0.0;

        return dotProduct / (norm1 * norm2);
    }

    private static String normalize(String s) {
        return s.replaceAll("[\\s\\pP\\pM\\pZ\\pC]", "").toLowerCase();
    }

    private static Map<String, Integer> bigramFreq(String s) {
        Map<String, Integer> freq = new HashMap<>();
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            String bigram = new String(chars, i, 2);
            freq.put(bigram, freq.getOrDefault(bigram, 0) + 1);
        }
        return freq;
    }

    private static double sumSquares(Collection<Integer> values) {
        double sum = 0;
        for (int v : values) sum += (double) v * v;
        return sum;
    }
}
