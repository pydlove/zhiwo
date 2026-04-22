package com.example.user.service;

import com.example.user.entity.Guide;
import com.example.user.mapper.GuideMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GuideService {
    private final GuideMapper guideMapper;

    public GuideService(GuideMapper guideMapper) {
        this.guideMapper = guideMapper;
    }

    public List<Guide> list() {
        return guideMapper.findAll();
    }

    public Guide getById(String id) {
        return guideMapper.findById(id);
    }

    public List<Guide> findRecommended() {
        return guideMapper.findRecommended();
    }
}
