package com.example.user.controller;

import com.example.user.entity.Result;
import com.example.user.entity.Blogger;
import com.example.user.service.BloggerService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bloggers")
@CrossOrigin(origins = "*")
public class BloggerController {
    private final BloggerService bloggerService;

    public BloggerController(BloggerService bloggerService) {
        this.bloggerService = bloggerService;
    }

    @GetMapping
    public Result<List<Blogger>> list(@RequestParam(required = false) String trackId) {
        if (trackId != null && !trackId.isEmpty()) {
            return Result.ok(bloggerService.listByTrack(trackId));
        }
        return Result.ok(java.util.Collections.emptyList());
    }

    @GetMapping("/{id}")
    public Result<Blogger> get(@PathVariable String id) {
        return Result.ok(bloggerService.getById(id));
    }
}
