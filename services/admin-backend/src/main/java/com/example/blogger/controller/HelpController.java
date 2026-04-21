package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.Help;
import com.example.blogger.service.HelpService;
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

    @PostMapping
    public Result<Void> save(@RequestBody Help help) {
        helpService.save(help);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Help help) {
        help.setId(id);
        helpService.save(help);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        helpService.delete(id);
        return Result.ok(null);
    }
}
