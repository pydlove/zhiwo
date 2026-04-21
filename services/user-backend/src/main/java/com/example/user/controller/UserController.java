package com.example.user.controller;

import com.example.user.entity.Result;
import com.example.user.entity.User;
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

    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
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
}
