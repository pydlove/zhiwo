package com.example.blogger.controller;

import com.example.blogger.entity.Activity;
import com.example.blogger.entity.Result;
import com.example.blogger.entity.User;
import com.example.blogger.service.ActivityService;
import com.example.blogger.service.EmailService;
import com.example.blogger.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/activities")
@CrossOrigin(origins = "*")
public class ActivityController {
    private final ActivityService activityService;
    private final UserService userService;
    private final EmailService emailService;

    public ActivityController(ActivityService activityService, UserService userService, EmailService emailService) {
        this.activityService = activityService;
        this.userService = userService;
        this.emailService = emailService;
    }

    @GetMapping
    public Result<List<Activity>> list() {
        return Result.ok(activityService.list());
    }

    @GetMapping("/{id}")
    public Result<Activity> get(@PathVariable String id) {
        return Result.ok(activityService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Activity activity) {
        activityService.save(activity);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Activity activity) {
        activity.setId(id);
        activityService.save(activity);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        activityService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/{id}/send-email")
    public Result<Map<String, Object>> sendEmail(@PathVariable String id, @RequestBody Map<String, List<String>> body) {
        List<String> userIds = body.get("userIds");
        if (userIds == null || userIds.isEmpty()) {
            return Result.error("请选择要推送的用户");
        }

        Activity activity = activityService.getById(id);
        if (activity == null) {
            return Result.error("活动不存在");
        }
        if (activity.getStatus() == null || activity.getStatus() != 1) {
            return Result.error("该活动已下架，无法推送");
        }

        int total = userIds.size();
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (String userId : userIds) {
            User user = userService.getById(userId);
            if (user == null) {
                failed++;
                errors.add("用户不存在: " + userId);
                continue;
            }
            if (user.getStatus() == null || user.getStatus() != 1) {
                failed++;
                errors.add("用户已禁用: " + user.getUsername());
                continue;
            }
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                failed++;
                errors.add("用户未设置邮箱: " + user.getUsername());
                continue;
            }

            try {
                emailService.sendActivityEmail(
                        user.getEmail(),
                        user.getUsername(),
                        activity.getTitle(),
                        activity.getContent(),
                        activity.getQrCodeUrl()
                );
                success++;
            } catch (Exception e) {
                failed++;
                errors.add(user.getUsername() + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("failed", failed);
        result.put("errors", errors);
        return Result.ok(result);
    }
}
