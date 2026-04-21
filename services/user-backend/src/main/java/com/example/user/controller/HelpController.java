package com.example.user.controller;

import com.example.user.entity.Result;
import com.example.user.entity.Help;
import com.example.user.service.HelpService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/helps")
@CrossOrigin(origins = "*")
public class HelpController {
    private final HelpService helpService;

    public HelpController(HelpService helpService) {
        this.helpService = helpService;
    }

    @GetMapping
    public Result<List<Help>> list() {
        return Result.ok(helpService.list());
    }

    @GetMapping("/{id}")
    public Result<Help> get(@PathVariable String id) {
        return Result.ok(helpService.getById(id));
    }
}
