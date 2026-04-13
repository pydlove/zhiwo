package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.Post;
import com.example.blogger.service.PostService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public Result<List<Post>> listByBlogger(@RequestParam String bloggerId) {
        return Result.ok(postService.listByBlogger(bloggerId));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Post post) {
        postService.save(post);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        postService.delete(id);
        return Result.ok(null);
    }
}
