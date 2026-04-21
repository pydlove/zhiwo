package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.Guide;
import com.example.blogger.service.GuideService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/guides")
@CrossOrigin(origins = "*")
public class GuideController {
    private final GuideService guideService;

    public GuideController(GuideService guideService) {
        this.guideService = guideService;
    }

    @GetMapping
    public Result<List<Guide>> list() {
        return Result.ok(guideService.list());
    }

    @GetMapping("/{id}")
    public Result<Guide> get(@PathVariable String id) {
        return Result.ok(guideService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Guide guide) {
        guideService.save(guide);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Guide guide) {
        guide.setId(id);
        guideService.save(guide);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        guideService.delete(id);
        return Result.ok(null);
    }
}
