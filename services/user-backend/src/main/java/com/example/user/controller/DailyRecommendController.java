package com.example.user.controller;

import com.example.user.entity.DailyRecommend;
import com.example.user.entity.Result;
import com.example.user.service.DailyRecommendService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/daily-recommends")
@CrossOrigin(origins = "*")
public class DailyRecommendController {
    private final DailyRecommendService dailyRecommendService;

    public DailyRecommendController(DailyRecommendService dailyRecommendService) {
        this.dailyRecommendService = dailyRecommendService;
    }

    @GetMapping
    public Result<List<DailyRecommend>> listByTrackAndPlatform(@RequestParam String trackId, @RequestParam String platform) {
        return Result.ok(dailyRecommendService.listByTrackAndPlatform(trackId, platform));
    }
}
