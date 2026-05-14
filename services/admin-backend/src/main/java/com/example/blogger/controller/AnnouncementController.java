package com.example.blogger.controller;

import com.example.blogger.entity.Announcement;
import com.example.blogger.entity.Result;
import com.example.blogger.service.AnnouncementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
@CrossOrigin(origins = "*")
public class AnnouncementController {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementController.class);

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public Result<List<Announcement>> list() {
        return Result.ok(announcementService.listAll());
    }

    @GetMapping("/{id}")
    public Result<Announcement> getById(@PathVariable String id) {
        Announcement announcement = announcementService.getById(id);
        if (announcement == null) {
            return Result.error("公告不存在");
        }
        return Result.ok(announcement);
    }

    @GetMapping("/active/{type}")
    public Result<Announcement> getActiveByType(@PathVariable String type) {
        Announcement announcement = announcementService.getActiveByType(type);
        return Result.ok(announcement);
    }

    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody Announcement announcement) {
        try {
            announcementService.save(announcement);
            return Result.ok(Map.of("id", announcement.getId()));
        } catch (Exception e) {
            log.error("[Announcement] 保存公告失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Announcement announcement) {
        try {
            announcement.setId(id);
            announcementService.save(announcement);
            return Result.ok(null);
        } catch (Exception e) {
            log.error("[Announcement] 更新公告失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        try {
            announcementService.delete(id);
            return Result.ok(null);
        } catch (Exception e) {
            log.error("[Announcement] 删除公告失败", e);
            return Result.error(e.getMessage());
        }
    }
}
