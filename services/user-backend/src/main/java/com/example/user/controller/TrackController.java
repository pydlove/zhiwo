package com.example.user.controller;

import com.example.user.entity.Result;
import com.example.user.entity.Track;
import com.example.user.entity.Blogger;
import com.example.user.entity.Post;
import com.example.user.service.TrackService;
import com.example.user.service.BloggerService;
import com.example.user.service.PostService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/tracks")
@CrossOrigin(origins = "*")
public class TrackController {
    private final TrackService trackService;
    private final BloggerService bloggerService;
    private final PostService postService;

    public TrackController(TrackService trackService, BloggerService bloggerService, PostService postService) {
        this.trackService = trackService;
        this.bloggerService = bloggerService;
        this.postService = postService;
    }

    @GetMapping
    public Result<List<Track>> list() {
        return Result.ok(trackService.list());
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable String id) {
        Track track = trackService.getById(id);
        List<Blogger> bloggers = bloggerService.listByTrack(id);
        List<Post> articles = postService.listByTrack(id);
        Map<String, Object> data = new HashMap<>();
        data.put("track", track);
        data.put("bloggers", bloggers);
        data.put("articles", articles);
        return Result.ok(data);
    }
}
