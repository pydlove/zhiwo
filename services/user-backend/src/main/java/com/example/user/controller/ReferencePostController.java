package com.example.user.controller;

import com.example.user.entity.ReferencePost;
import com.example.user.entity.Result;
import com.example.user.service.ReferencePostService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
    public Result<List<ReferencePost>> listByTrackAndPlatform(@RequestParam String trackId, @RequestParam String platform) {
        return Result.ok(referencePostService.listByTrackAndPlatform(trackId, platform));
    }
}
