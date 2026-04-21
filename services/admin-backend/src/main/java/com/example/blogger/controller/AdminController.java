package com.example.blogger.controller;

import com.example.blogger.entity.Admin;
import com.example.blogger.entity.Result;
import com.example.blogger.service.AdminService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admins")
@CrossOrigin(origins = "*")
public class AdminController {
    private final AdminService adminService;
    private final PasswordEncoder passwordEncoder;

    public AdminController(AdminService adminService, PasswordEncoder passwordEncoder) {
        this.adminService = adminService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public Result<List<Admin>> list() {
        return Result.ok(adminService.list());
    }

    @GetMapping("/{id}")
    public Result<Admin> get(@PathVariable String id) {
        return Result.ok(adminService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Admin admin) {
        if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }
        adminService.save(admin);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Admin admin) {
        Admin existing = adminService.getById(id);
        if (existing == null) {
            return Result.error("管理员不存在");
        }
        if (admin.getUsername() != null) existing.setUsername(admin.getUsername());
        if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(admin.getPassword()));
        }
        if (admin.getStatus() != null) existing.setStatus(admin.getStatus());
        if (admin.getPhone() != null) existing.setPhone(admin.getPhone());
        if (admin.getEmail() != null) existing.setEmail(admin.getEmail());
        if (admin.getWxId() != null) existing.setWxId(admin.getWxId());
        if (admin.getAiLimit() != null) existing.setAiLimit(admin.getAiLimit());
        if (admin.getExpireDate() != null) existing.setExpireDate(admin.getExpireDate());
        if (admin.getRemark() != null) existing.setRemark(admin.getRemark());
        if (admin.getName() != null) existing.setName(admin.getName());
        if (admin.getRole() != null) existing.setRole(admin.getRole());
        adminService.save(existing);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        adminService.delete(id);
        return Result.ok(null);
    }
}
