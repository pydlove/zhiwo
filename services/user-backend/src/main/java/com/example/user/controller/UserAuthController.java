package com.example.user.controller;

import com.example.user.entity.Result;
import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class UserAuthController {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserAuthController(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> req) {
        String username = req.get("username");
        String password = req.get("password");
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return Result.error("账号或密码错误");
        }

        String storedPassword = user.getPassword();
        boolean matched;
        if (storedPassword != null && storedPassword.startsWith("$2a$")) {
            matched = passwordEncoder.matches(password, storedPassword);
        } else {
            // 兼容旧明文密码：验证通过后自动升级为 BCrypt
            matched = password.equals(storedPassword);
            if (matched) {
                userMapper.updatePassword(user.getId(), passwordEncoder.encode(password));
            }
        }

        if (!matched) {
            return Result.error("账号或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            return Result.error("账号已被禁用");
        }
        userMapper.updateLastLogin(user.getId(), LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        Map<String, Object> data = new HashMap<>();
        data.put("token", "user-token-" + user.getId());
        data.put("user", user);
        return Result.ok(data);
    }
}
