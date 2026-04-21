package com.example.user.controller;

import com.example.user.entity.Result;
import com.example.user.entity.SubscriptionPost;
import com.example.user.service.SubscriptionPostService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscription-posts")
@CrossOrigin(origins = "*")
public class SubscriptionPostController {
    private final SubscriptionPostService service;

    public SubscriptionPostController(SubscriptionPostService service) {
        this.service = service;
    }

    @GetMapping("/latest")
    public Result<SubscriptionPost> getLatest(
            @RequestParam String userId,
            @RequestParam String trackId) {
        return Result.ok(service.getLatestByUserAndTrack(userId, trackId));
    }

    @PostMapping("/{id}/used")
    public Result<Void> markUsed(@PathVariable String id) {
        service.markUsed(id);
        return Result.ok(null);
    }
}
