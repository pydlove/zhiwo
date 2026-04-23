package com.example.user.controller;

import com.example.user.entity.Result;
import com.example.user.entity.CreationRecord;
import com.example.user.entity.User;
import com.example.user.mapper.CreationRecordMapper;
import com.example.user.service.CreationRecordService;
import com.example.user.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/creation-records")
@CrossOrigin(origins = "*")
public class CreationRecordController {
    private final CreationRecordService creationRecordService;
    private final UserService userService;
    private final CreationRecordMapper creationRecordMapper;

    public CreationRecordController(CreationRecordService creationRecordService, UserService userService, CreationRecordMapper creationRecordMapper) {
        this.creationRecordService = creationRecordService;
        this.userService = userService;
        this.creationRecordMapper = creationRecordMapper;
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
        User user = userService.getById(record.getUserId());
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (user.getExpireDate() != null && user.getExpireDate().isBefore(LocalDate.now())) {
            return Result.error("账号已到期，请联系管理员续费");
        }
        Integer aiLimit = user.getAiLimit();
        if (aiLimit != null && aiLimit > 0) {
            int todayCount = creationRecordMapper.countByUserIdAndDate(record.getUserId(), LocalDate.now().toString());
            if (todayCount >= aiLimit) {
                return Result.error("今日 AI 创作次数已达上限 " + aiLimit + " 次");
            }
        }
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
