package com.example.user.controller;

import com.example.user.entity.Result;
import com.example.user.entity.Post;
import com.example.user.service.PostService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public Result<List<Post>> list(@RequestParam(required = false) String bloggerId) {
        if (bloggerId != null && !bloggerId.isEmpty()) {
            return Result.ok(postService.listByBlogger(bloggerId));
        }
        return Result.ok(java.util.Collections.emptyList());
    }

    @GetMapping("/recommendations")
    public Result<List<Map<String, Object>>> recommendations(@RequestParam(required = false) String trackId) {
        if (trackId != null && !trackId.isEmpty()) {
            return Result.ok(postService.listRecommendationsByTrack(trackId));
        }
        return Result.ok(postService.listRecommendations());
    }

    @GetMapping("/search")
    public Result<List<Post>> search(
            @RequestParam(required = false) String trackId,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String keyword) {
        return Result.ok(postService.search(trackId, platform, keyword));
    }
}
