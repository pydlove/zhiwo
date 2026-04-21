package com.example.user.controller;

import com.example.user.entity.Result;
import com.example.user.entity.Style;
import com.example.user.service.StyleService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/styles")
@CrossOrigin(origins = "*")
public class StyleController {
    private final StyleService styleService;

    public StyleController(StyleService styleService) {
        this.styleService = styleService;
    }

    @GetMapping
    public Result<List<Style>> list() {
        return Result.ok(styleService.listEnabled());
    }

    @GetMapping("/{id}")
    public Result<Style> get(@PathVariable String id) {
        return Result.ok(styleService.getById(id));
    }
}
