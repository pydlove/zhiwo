package com.example.blogger.controller;

import com.example.blogger.entity.ReferencePost;
import com.example.blogger.entity.Result;
import com.example.blogger.service.ReferencePostService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reference-posts")
@CrossOrigin(origins = "*")
public class ReferencePostController {
    private final ReferencePostService referencePostService;

    public ReferencePostController(ReferencePostService referencePostService) {
        this.referencePostService = referencePostService;
    }

    @GetMapping
    public Result<List<ReferencePost>> list() {
        return Result.ok(referencePostService.list());
    }

    @GetMapping("/{id}")
    public Result<ReferencePost> get(@PathVariable String id) {
        return Result.ok(referencePostService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody ReferencePost r) {
        referencePostService.save(r);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody ReferencePost r) {
        r.setId(id);
        referencePostService.save(r);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        referencePostService.delete(id);
        return Result.ok(null);
    }
}
