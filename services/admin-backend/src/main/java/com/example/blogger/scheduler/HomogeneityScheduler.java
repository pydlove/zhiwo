package com.example.blogger.scheduler;

import com.example.blogger.entity.User;
import com.example.blogger.entity.UserHomogeneity;
import com.example.blogger.mapper.TitleRecommendationMapper;
import com.example.blogger.mapper.UserHomogeneityMapper;
import com.example.blogger.mapper.UserMapper;
import com.example.blogger.util.TextSimilarityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户同质化程度定时计算任务
 * 每天凌晨 01:00 执行，为所有活跃用户计算历史文章标题的同质化程度
 */
@Component
public class HomogeneityScheduler {

    private static final Logger log = LoggerFactory.getLogger(HomogeneityScheduler.class);

    private final UserMapper userMapper;
    private final TitleRecommendationMapper titleRecommendationMapper;
    private final UserHomogeneityMapper userHomogeneityMapper;

    public HomogeneityScheduler(UserMapper userMapper,
                                TitleRecommendationMapper titleRecommendationMapper,
                                UserHomogeneityMapper userHomogeneityMapper) {
        this.userMapper = userMapper;
        this.titleRecommendationMapper = titleRecommendationMapper;
        this.userHomogeneityMapper = userHomogeneityMapper;
    }

    /**
     * 每天凌晨 1:00 执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void calculateAllUsersHomogeneity() {
        log.info("[HomogeneityScheduler] 开始计算所有用户的同质化程度");

        List<User> users = userMapper.findAll().stream()
                .filter(u -> u.getStatus() != null && u.getStatus() == 1)
                .filter(u -> u.getIsDeleted() == null || u.getIsDeleted() != 1)
                .filter(u -> u.getUserType() != null && u.getUserType() >= 1 && u.getUserType() <= 3)
                .collect(Collectors.toList());

        int processed = 0;
        int failed = 0;

        for (User user : users) {
            try {
                int score = calculateUserHomogeneity(user.getId());
                int historyCount = getUserHistoryCount(user.getId());

                UserHomogeneity record = userHomogeneityMapper.findByUserId(user.getId());
                if (record == null) {
                    record = new UserHomogeneity();
                    record.setId(UUID.randomUUID().toString().replace("-", ""));
                    record.setUserId(user.getId());
                    record.setHomogeneityScore(score);
                    record.setHistoryCount(historyCount);
                    record.setCalculatedAt(LocalDateTime.now());
                    userHomogeneityMapper.insert(record);
                } else {
                    record.setHomogeneityScore(score);
                    record.setHistoryCount(historyCount);
                    record.setCalculatedAt(LocalDateTime.now());
                    userHomogeneityMapper.updateByUserId(record);
                }
                processed++;
            } catch (Exception e) {
                failed++;
                log.error("[HomogeneityScheduler] 计算用户同质化失败: userId={}, error={}", user.getId(), e.getMessage(), e);
            }
        }

        log.info("[HomogeneityScheduler] 完成，共处理 {} 个用户，失败 {} 个", processed, failed);
    }

    private int calculateUserHomogeneity(String userId) {
        List<Map<String, Object>> list = titleRecommendationMapper.findHistoryByUserId(userId);
        if (list == null || list.size() < 2) {
            return 0;
        }

        List<String> titles = new ArrayList<>();
        for (Map<String, Object> item : list) {
            String t = item.get("titleName") != null ? item.get("titleName").toString() : "";
            if (!t.isEmpty()) {
                titles.add(t);
            }
        }

        if (titles.size() < 2) {
            return 0;
        }

        double totalSim = 0;
        int pairCount = 0;
        for (int i = 0; i < titles.size(); i++) {
            for (int j = i + 1; j < titles.size(); j++) {
                totalSim += TextSimilarityUtil.similarity(titles.get(i), titles.get(j));
                pairCount++;
            }
        }

        double avgSim = pairCount > 0 ? totalSim / pairCount : 0;
        return (int) Math.round(avgSim * 100);
    }

    private int getUserHistoryCount(String userId) {
        List<Map<String, Object>> list = titleRecommendationMapper.findHistoryByUserId(userId);
        if (list == null) return 0;
        int count = 0;
        for (Map<String, Object> item : list) {
            String t = item.get("titleName") != null ? item.get("titleName").toString() : "";
            if (!t.isEmpty()) count++;
        }
        return count;
    }
}
