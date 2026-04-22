package com.example.user.controller;

import com.example.user.entity.Result;
import com.example.user.entity.Guide;
import com.example.user.service.GuideService;
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

    @GetMapping("/recommended")
    public Result<List<Guide>> listRecommended() {
        return Result.ok(guideService.findRecommended());
    }
}
