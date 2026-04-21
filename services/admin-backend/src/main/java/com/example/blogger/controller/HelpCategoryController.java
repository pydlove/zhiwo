package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.HelpCategory;
import com.example.blogger.service.HelpCategoryService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/help-categories")
@CrossOrigin(origins = "*")
public class HelpCategoryController {
    private final HelpCategoryService helpCategoryService;

    public HelpCategoryController(HelpCategoryService helpCategoryService) {
        this.helpCategoryService = helpCategoryService;
    }

    @GetMapping
    public Result<List<HelpCategory>> list() {
        return Result.ok(helpCategoryService.list());
    }

    @GetMapping("/{id}")
    public Result<HelpCategory> get(@PathVariable String id) {
        return Result.ok(helpCategoryService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody HelpCategory category) {
        helpCategoryService.save(category);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody HelpCategory category) {
        category.setId(id);
        helpCategoryService.save(category);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        helpCategoryService.delete(id);
        return Result.ok(null);
    }
}
