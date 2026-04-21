package com.example.blogger.controller;

import com.example.blogger.entity.Admin;
import com.example.blogger.entity.Result;
import com.example.blogger.entity.Role;
import com.example.blogger.mapper.AdminMapper;
import com.example.blogger.mapper.RoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AdminAuthController {
    private final AdminMapper adminMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthController(AdminMapper adminMapper, RoleMapper roleMapper, PasswordEncoder passwordEncoder) {
        this.adminMapper = adminMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> req) {
        String username = req.get("username");
        String password = req.get("password");
        Admin admin = adminMapper.findByUsername(username);
        if (admin == null) {
            return Result.error("账号或密码错误");
        }

        String storedPassword = admin.getPassword();
        boolean matched;
        if (storedPassword != null && storedPassword.startsWith("$2a$")) {
            matched = passwordEncoder.matches(password, storedPassword);
        } else {
            matched = password.equals(storedPassword);
            if (matched) {
                adminMapper.updatePassword(admin.getId(), passwordEncoder.encode(password));
            }
        }

        if (!matched) {
            return Result.error("账号或密码错误");
        }

        if (admin.getStatus() == null || admin.getStatus() != 1) {
            return Result.error("账号已被禁用");
        }
        adminMapper.updateLastLogin(admin.getId(), LocalDateTime.now());
        admin.setLastLogin(LocalDateTime.now());
        Map<String, Object> data = new HashMap<>();
        data.put("token", "admin-token-" + admin.getId());
        data.put("user", admin);
        Role role = roleMapper.findByName(admin.getRole());
        data.put("permissions", role != null ? role.getPermissions() : "[]");
        return Result.ok(data);
    }
}
