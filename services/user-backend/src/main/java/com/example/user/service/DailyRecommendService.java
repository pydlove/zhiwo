package com.example.user.service;

import com.example.user.entity.DailyRecommend;
import com.example.user.mapper.DailyRecommendMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DailyRecommendService {
    private final DailyRecommendMapper dailyRecommendMapper;

    public DailyRecommendService(DailyRecommendMapper dailyRecommendMapper) {
        this.dailyRecommendMapper = dailyRecommendMapper;
    }

    public List<DailyRecommend> listByTrackAndPlatform(String trackId, String platform) {
        return dailyRecommendMapper.findByTrackAndPlatform(trackId, platform);
    }
}
