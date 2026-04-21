package com.example.user.controller;

import com.example.user.entity.Result;
import com.example.user.entity.User;
import com.example.user.service.EmailService;
import com.example.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserController(UserService userService, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @GetMapping
    public Result<List<User>> list() {
        return Result.ok(userService.list());
    }

    @GetMapping("/{id}")
    public Result<User> get(@PathVariable String id) {
        return Result.ok(userService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody User user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userService.save(user);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody User user) {
        User existing = userService.getById(id);
        if (existing == null) {
            return Result.error("用户不存在");
        }
        if (user.getUsername() != null) existing.setUsername(user.getUsername());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (user.getStatus() != null) existing.setStatus(user.getStatus());
        if (user.getPhone() != null) existing.setPhone(user.getPhone());
        if (user.getEmail() != null) existing.setEmail(user.getEmail());
        if (user.getWxId() != null) existing.setWxId(user.getWxId());
        if (user.getAiLimit() != null) existing.setAiLimit(user.getAiLimit());
        if (user.getTrackLimit() != null) existing.setTrackLimit(user.getTrackLimit());
        if (user.getPlatformLimit() != null) existing.setPlatformLimit(user.getPlatformLimit());
        if (user.getExpireDate() != null) existing.setExpireDate(user.getExpireDate());
        if (user.getRemark() != null) existing.setRemark(user.getRemark());
        if (user.getTemplate() != null) existing.setTemplate(user.getTemplate());
        if (user.getCanSetEmail() != null) existing.setCanSetEmail(user.getCanSetEmail());
        if (user.getEmailReceive() != null) existing.setEmailReceive(user.getEmailReceive());
        userService.save(existing);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        userService.delete(id);
        return Result.ok(null);
    }

    @PutMapping("/{id}/avatar")
    public Result<Void> updateAvatar(@PathVariable String id, @RequestBody Map<String, String> req) {
        User existing = userService.getById(id);
        if (existing == null) {
            return Result.error("用户不存在");
        }
        existing.setAvatar(req.get("avatar"));
        userService.save(existing);
        return Result.ok(null);
    }

    @GetMapping("/{id}/email-config")
    public Result<Map<String, Object>> getEmailConfig(@PathVariable String id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.ok(Map.of(
            "email", user.getEmail() != null ? user.getEmail() : "",
            "emailReceive", user.getEmailReceive() != null ? user.getEmailReceive() : 0,
            "canSetEmail", user.getCanSetEmail() != null ? user.getCanSetEmail() : 0
        ));
    }

    @PutMapping("/{id}/email")
    public Result<Void> updateEmailConfig(@PathVariable String id, @RequestBody Map<String, Object> req) {
        User existing = userService.getById(id);
        if (existing == null) {
            return Result.error("用户不存在");
        }
        Object emailObj = req.get("email");
        if (emailObj != null) {
            existing.setEmail(emailObj.toString());
        }
        Object receiveObj = req.get("emailReceive");
        if (receiveObj != null) {
            existing.setEmailReceive(Integer.parseInt(receiveObj.toString()));
        }
        userService.save(existing);
        return Result.ok(null);
    }

    @PostMapping("/{id}/email/test")
    public Result<Void> sendTestEmail(@PathVariable String id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            return Result.error("请先配置邮箱地址");
        }
        try {
            emailService.sendTestEmail(user.getEmail());
            return Result.ok(null);
        } catch (Exception e) {
            return Result.error("邮件发送失败: " + e.getMessage());
        }
    }
}
