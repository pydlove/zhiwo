package com.example.blogger.service;

import com.example.blogger.entity.SubscriptionPost;
import com.example.blogger.mapper.SubscriptionPostMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SubscriptionPostService {
    private final SubscriptionPostMapper mapper;

    public SubscriptionPostService(SubscriptionPostMapper mapper) {
        this.mapper = mapper;
    }

    public List<SubscriptionPost> list() {
        return mapper.findAll();
    }

    public List<SubscriptionPost> listByCondition(Map<String, Object> params) {
        return mapper.findByCondition(params);
    }

    public List<SubscriptionPost> listByUserId(String userId) {
        return mapper.findByUserId(userId);
    }

    public SubscriptionPost getById(String id) {
        return mapper.findById(id);
    }

    public SubscriptionPost getLatestByUserAndTrack(String userId, String trackId) {
        return mapper.findLatestByUserAndTrack(userId, trackId);
    }

    public void save(SubscriptionPost p) {
        if (p.getId() == null || p.getId().isEmpty()) {
            p.setId(UUID.randomUUID().toString().replace("-", ""));
            mapper.insert(p);
        } else {
            mapper.update(p);
        }
    }

    public void delete(String id) {
        mapper.delete(id);
    }
}
