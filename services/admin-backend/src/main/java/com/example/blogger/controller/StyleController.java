package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.Style;
import com.example.blogger.service.StyleService;
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
        return Result.ok(styleService.list());
    }

    @GetMapping("/{id}")
    public Result<Style> get(@PathVariable String id) {
        return Result.ok(styleService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Style style) {
        styleService.save(style);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Style style) {
        Style existing = styleService.getById(id);
        if (existing == null) {
            return Result.error("模板不存在");
        }
        if (style.getName() != null) existing.setName(style.getName());
        if (style.getScene() != null) existing.setScene(style.getScene());
        if (style.getIsDefault() != null) existing.setIsDefault(style.getIsDefault());
        if (style.getStatus() != null) existing.setStatus(style.getStatus());
        if (style.getStyleJson() != null) existing.setStyleJson(style.getStyleJson());
        styleService.save(existing);
        if (Integer.valueOf(1).equals(style.getIsDefault())) {
            styleService.setDefault(id);
        }
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        styleService.delete(id);
        return Result.ok(null);
    }
}
