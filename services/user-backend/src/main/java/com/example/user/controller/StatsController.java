package com.example.user.controller;

import com.example.user.entity.Result;
import com.example.user.mapper.StatsMapper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")
public class StatsController {
    private final StatsMapper statsMapper;

    public StatsController(StatsMapper statsMapper) {
        this.statsMapper = statsMapper;
    }

    @GetMapping
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> data = new HashMap<>();
        int today = statsMapper.countTodayPosts();
        int yesterday = statsMapper.countYesterdayPosts();
        data.put("todayNewPosts", today);
        data.put("yesterdayNewPosts", yesterday);
        data.put("totalTracks", statsMapper.countTracks());
        data.put("totalBloggers", statsMapper.countBloggers());
        data.put("totalPosts", statsMapper.countPosts());
        data.put("weekNewTracks", statsMapper.countWeekTracks());
        data.put("weekNewBloggers", statsMapper.countWeekBloggers());
        data.put("weekNewPosts", statsMapper.countWeekPosts());
        data.put("totalSubscriptionPosts", statsMapper.countSubscriptionPosts());
        return Result.ok(data);
    }
}
