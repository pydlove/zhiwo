package com.example.blogger.controller;

import com.example.blogger.entity.DailyRecommend;
import com.example.blogger.entity.Result;
import com.example.blogger.service.DailyRecommendService;
import org.springframework.web.bind.annotation.*;
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
    public Result<List<DailyRecommend>> list() {
        return Result.ok(dailyRecommendService.list());
    }

    @GetMapping("/{id}")
    public Result<DailyRecommend> get(@PathVariable String id) {
        return Result.ok(dailyRecommendService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody DailyRecommend d) {
        dailyRecommendService.save(d);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody DailyRecommend d) {
        d.setId(id);
        dailyRecommendService.save(d);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        dailyRecommendService.delete(id);
        return Result.ok(null);
    }
}
