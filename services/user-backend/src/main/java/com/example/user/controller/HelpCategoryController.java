package com.example.user.controller;

import com.example.user.entity.Result;
import com.example.user.entity.HelpCategory;
import com.example.user.service.HelpCategoryService;
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
}
