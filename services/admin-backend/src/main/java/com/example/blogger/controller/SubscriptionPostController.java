package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.SubscriptionPost;
import com.example.blogger.service.SubscriptionPostService;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription-posts")
@CrossOrigin(origins = "*")
public class SubscriptionPostController {
    private final SubscriptionPostService service;

    public SubscriptionPostController(SubscriptionPostService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<SubscriptionPost>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String trackId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("userId", userId);
        params.put("trackId", trackId);
        params.put("status", status);
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        return Result.ok(service.listByCondition(params));
    }

    @GetMapping("/{id}")
    public Result<SubscriptionPost> get(@PathVariable String id) {
        return Result.ok(service.getById(id));
    }

    @GetMapping("/user/{userId}")
    public Result<List<SubscriptionPost>> listByUser(@PathVariable String userId) {
        return Result.ok(service.listByUserId(userId));
    }

    @GetMapping("/latest")
    public Result<SubscriptionPost> getLatest(
            @RequestParam String userId,
            @RequestParam String trackId) {
        return Result.ok(service.getLatestByUserAndTrack(userId, trackId));
    }

    @PostMapping
    public Result<Void> save(@RequestBody SubscriptionPost p) {
        service.save(p);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody SubscriptionPost p) {
        p.setId(id);
        service.save(p);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        service.delete(id);
        return Result.ok(null);
    }
}
