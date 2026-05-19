package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.WritingStyle;
import com.example.blogger.service.WritingStyleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/writing-styles")
@CrossOrigin(origins = "*")
public class WritingStyleController {

    private final WritingStyleService writingStyleService;

    public WritingStyleController(WritingStyleService writingStyleService) {
        this.writingStyleService = writingStyleService;
    }

    @GetMapping
    public Result<List<WritingStyle>> list(@RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty()) {
            return Result.ok(writingStyleService.listByCategory(category));
        }
        return Result.ok(writingStyleService.list());
    }

    @GetMapping("/{id}")
    public Result<WritingStyle> get(@PathVariable String id) {
        return Result.ok(writingStyleService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody WritingStyle writingStyle) {
        writingStyleService.save(writingStyle);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        writingStyleService.delete(id);
        return Result.ok(null);
    }

    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.ok(writingStyleService.categories());
    }
}
