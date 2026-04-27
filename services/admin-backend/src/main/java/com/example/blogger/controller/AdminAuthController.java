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

    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody Map<String, String> req) {
        String adminId = req.get("adminId");
        String oldPassword = req.get("oldPassword");
        String newPassword = req.get("newPassword");

        if (adminId == null || adminId.isEmpty()) {
            return Result.error("未登录");
        }
        if (oldPassword == null || oldPassword.isEmpty()) {
            return Result.error("请输入当前密码");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return Result.error("请输入新密码");
        }
        if (newPassword.length() < 6) {
            return Result.error("新密码长度不能少于6位");
        }

        Admin admin = adminMapper.findById(adminId);
        if (admin == null) {
            return Result.error("管理员不存在");
        }

        String storedPassword = admin.getPassword();
        boolean matched;
        if (storedPassword != null && storedPassword.startsWith("$2a$")) {
            matched = passwordEncoder.matches(oldPassword, storedPassword);
        } else {
            matched = oldPassword.equals(storedPassword);
        }

        if (!matched) {
            return Result.error("当前密码错误");
        }

        adminMapper.updatePassword(adminId, passwordEncoder.encode(newPassword));
        return Result.ok(null);
    }
}
