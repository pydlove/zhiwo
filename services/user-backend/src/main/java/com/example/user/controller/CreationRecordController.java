package com.example.user.controller;

import com.example.user.entity.Result;
import com.example.user.entity.CreationRecord;
import com.example.user.service.CreationRecordService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/creation-records")
@CrossOrigin(origins = "*")
public class CreationRecordController {
    private final CreationRecordService creationRecordService;

    public CreationRecordController(CreationRecordService creationRecordService) {
        this.creationRecordService = creationRecordService;
    }

    @GetMapping
    public Result<List<CreationRecord>> list(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String trackId,
            @RequestParam(required = false) String date) {
        return Result.ok(creationRecordService.list(userId, trackId, date));
    }

    @GetMapping("/{id}")
    public Result<CreationRecord> get(@PathVariable String id) {
        return Result.ok(creationRecordService.getById(id));
    }

    @PostMapping
    public Result<CreationRecord> save(@RequestBody CreationRecord record) {
        CreationRecord saved = creationRecordService.save(record);
        return Result.ok(saved);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody CreationRecord record) {
        record.setId(id);
        creationRecordService.save(record);
        return Result.ok(null);
    }

    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable String id) {
        creationRecordService.markReviewed(id);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        creationRecordService.delete(id);
        return Result.ok(null);
    }
}
