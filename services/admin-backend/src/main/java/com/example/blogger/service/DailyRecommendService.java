package com.example.blogger.service;

import com.example.blogger.entity.DailyRecommend;
import com.example.blogger.mapper.DailyRecommendMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class DailyRecommendService {
    private final DailyRecommendMapper dailyRecommendMapper;

    public DailyRecommendService(DailyRecommendMapper dailyRecommendMapper) {
        this.dailyRecommendMapper = dailyRecommendMapper;
    }

    public List<DailyRecommend> list() {
        return dailyRecommendMapper.findAll();
    }

    public DailyRecommend getById(String id) {
        return dailyRecommendMapper.findById(id);
    }

    public void save(DailyRecommend d) {
        if (d.getId() == null || d.getId().isEmpty()) {
            d.setId(UUID.randomUUID().toString().replace("-", ""));
            if (d.getStatus() == null || d.getStatus().isEmpty()) {
                d.setStatus("已上架");
            }
            dailyRecommendMapper.insert(d);
        } else {
            dailyRecommendMapper.update(d);
        }
    }

    public void delete(String id) {
        dailyRecommendMapper.delete(id);
    }
}
