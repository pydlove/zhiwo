package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.Guide;
import com.example.blogger.service.GuideService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/guides")
@CrossOrigin(origins = "*")
public class GuideController {
    private final GuideService guideService;

    public GuideController(GuideService guideService) {
        this.guideService = guideService;
    }

    @GetMapping
    public Result<List<Guide>> list(@RequestParam(required = false) Integer isRecommended) {
        List<Guide> list = guideService.list();
        if (isRecommended != null) {
            list = list.stream()
                .filter(g -> isRecommended.equals(g.getIsRecommended()))
                .toList();
        }
        return Result.ok(list);
    }

    @GetMapping("/{id}")
    public Result<Guide> get(@PathVariable String id) {
        return Result.ok(guideService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Guide guide) {
        if (guide.getIsRecommended() == null) {
            guide.setIsRecommended(0);
        }
        guideService.save(guide);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Guide guide) {
        guide.setId(id);
        if (guide.getIsRecommended() == null) {
            guide.setIsRecommended(0);
        }
        guideService.save(guide);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        guideService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/batch-recommended")
    public Result<Void> batchUpdateRecommended(@RequestBody Map<String, Object> req) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) req.get("ids");
        Object recObj = req.get("isRecommended");
        if (ids == null || ids.isEmpty() || recObj == null) {
            return Result.error("参数错误");
        }
        int isRecommended = Integer.parseInt(recObj.toString());
        guideService.batchUpdateRecommended(ids, isRecommended);
        return Result.ok(null);
    }
}
