package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.Blogger;
import com.example.blogger.service.BloggerService;
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
        return Result.ok(bloggerService.listAll());
    }

    @GetMapping("/{id}")
    public Result<Blogger> get(@PathVariable String id) {
        return Result.ok(bloggerService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Blogger blogger) {
        bloggerService.save(blogger);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Blogger blogger) {
        blogger.setId(id);
        bloggerService.save(blogger);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        bloggerService.delete(id);
        return Result.ok(null);
    }
}
