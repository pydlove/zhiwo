package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.Help;
import com.example.blogger.entity.User;
import com.example.blogger.service.EmailService;
import com.example.blogger.service.HelpService;
import com.example.blogger.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/helps")
@CrossOrigin(origins = "*")
public class HelpController {
    private final HelpService helpService;
    private final UserService userService;
    private final EmailService emailService;

    public HelpController(HelpService helpService, UserService userService, EmailService emailService) {
        this.helpService = helpService;
        this.userService = userService;
        this.emailService = emailService;
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

    @PostMapping("/{id}/send-email")
    public Result<Map<String, Object>> sendEmail(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            Help help = helpService.getById(id);
            if (help == null) {
                return Result.error("帮助文档不存在");
            }
            if (!"已上架".equals(help.getStatus())) {
                return Result.error("该文档已下架，无法推送");
            }

            @SuppressWarnings("unchecked")
            List<String> userIds = (List<String>) body.get("userIds");
            if (userIds == null || userIds.isEmpty()) {
                return Result.error("请选择要推送的用户");
            }

            int total = userIds.size();
            int success = 0;
            int failed = 0;
            List<Map<String, String>> errors = new ArrayList<>();

            for (String userId : userIds) {
                User user = userService.getById(userId);
                if (user == null) {
                    failed++;
                    errors.add(Map.of("userId", userId, "reason", "用户不存在"));
                    continue;
                }
                if (user.getStatus() == null || user.getStatus() != 1) {
                    failed++;
                    errors.add(Map.of("userName", user.getUsername(), "reason", "用户已禁用"));
                    continue;
                }
                if (user.getEmail() == null || user.getEmail().isEmpty()) {
                    failed++;
                    errors.add(Map.of("userName", user.getUsername(), "reason", "用户未设置邮箱"));
                    continue;
                }

                try {
                    emailService.sendHelpArticleEmail(
                            user.getEmail(),
                            user.getUsername(),
                            help.getTitle(),
                            help.getContent(),
                            help.getCategory()
                    );
                    success++;
                } catch (Exception e) {
                    failed++;
                    errors.add(Map.of("userName", user.getUsername(), "reason", "邮件发送失败：" + e.getMessage()));
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total", total);
            result.put("success", success);
            result.put("failed", failed);
            result.put("errors", errors);
            return Result.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("推送失败：" + e.getMessage());
        }
    }
}
