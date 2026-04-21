package com.example.user.service;

import com.example.user.entity.SubscriptionPost;
import com.example.user.mapper.SubscriptionPostMapper;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionPostService {
    private final SubscriptionPostMapper mapper;

    public SubscriptionPostService(SubscriptionPostMapper mapper) {
        this.mapper = mapper;
    }

    public SubscriptionPost getLatestByUserAndTrack(String userId, String trackId) {
        return mapper.findLatestByUserAndTrack(userId, trackId);
    }

    public void markUsed(String id) {
        mapper.markUsedById(id);
    }
}
