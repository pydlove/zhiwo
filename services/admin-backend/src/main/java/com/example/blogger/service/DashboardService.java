package com.example.blogger.service;

import com.example.blogger.mapper.*;
import com.example.blogger.entity.CreationRecord;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DashboardService {
    private final TrackMapper trackMapper;
    private final BloggerMapper bloggerMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final CreationRecordMapper creationRecordMapper;

    public DashboardService(TrackMapper trackMapper, BloggerMapper bloggerMapper, PostMapper postMapper, UserMapper userMapper, CreationRecordMapper creationRecordMapper) {
        this.trackMapper = trackMapper;
        this.bloggerMapper = bloggerMapper;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.creationRecordMapper = creationRecordMapper;
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTracks", trackMapper.countTracks());
        stats.put("totalBloggers", bloggerMapper.countBloggers());
        stats.put("totalPosts", postMapper.countPosts());
        stats.put("totalUsers", userMapper.countUsers());
        stats.put("pendingCreations", creationRecordMapper.countPendingCreations());
        stats.put("todayCreations", creationRecordMapper.countTodayCreations());
        stats.put("topTracks", trackMapper.findTopTracks(5));
        stats.put("platformDistribution", postMapper.countByPlatform());
        return stats;
    }
}
