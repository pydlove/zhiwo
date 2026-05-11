package com.example.blogger.controller;

import com.example.blogger.entity.AiFlavorRule;
import com.example.blogger.entity.Result;
import com.example.blogger.service.AiFlavorRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-flavor-rules")
public class AiFlavorRuleController {

    @Autowired
    private AiFlavorRuleService service;

    @GetMapping
    public Result<List<AiFlavorRule>> list() {
        return Result.ok(service.list());
    }

    @PostMapping
    public Result<Void> save(@RequestBody AiFlavorRule rule) {
        if (rule.getRuleFrom() == null || rule.getRuleFrom().trim().isEmpty()) {
            return Result.error("替换前不能为空");
        }
        service.save(rule);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        service.delete(id);
        return Result.ok(null);
    }
}