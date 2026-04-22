package com.example.blogger.service;

import com.example.blogger.entity.Guide;
import com.example.blogger.mapper.GuideMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

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

    public void save(Guide guide) {
        if (guide.getId() == null || guide.getId().isEmpty()) {
            guide.setId(UUID.randomUUID().toString().replace("-", ""));
            guideMapper.insert(guide);
        } else {
            guideMapper.update(guide);
        }
    }

    public void delete(String id) {
        guideMapper.delete(id);
    }

    public int batchUpdateRecommended(List<String> ids, Integer isRecommended) {
        return guideMapper.batchUpdateRecommended(ids, isRecommended);
    }
}
