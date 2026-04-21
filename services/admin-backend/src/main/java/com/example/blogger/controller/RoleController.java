package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.Role;
import com.example.blogger.service.RoleService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public Result<List<Role>> list() {
        return Result.ok(roleService.list());
    }

    @GetMapping("/{id}")
    public Result<Role> get(@PathVariable String id) {
        return Result.ok(roleService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Role role) {
        roleService.save(role);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Role role) {
        Role existing = roleService.getById(id);
        if (existing == null) {
            return Result.error("角色不存在");
        }
        if (role.getName() != null) existing.setName(role.getName());
        if (role.getPermissions() != null) existing.setPermissions(role.getPermissions());
        roleService.save(existing);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        roleService.delete(id);
        return Result.ok(null);
    }

    @GetMapping("/admin-counts")
    public Result<Map<String, Integer>> adminCounts() {
        List<Role> roles = roleService.list();
        Map<String, Integer> counts = new HashMap<>();
        for (Role role : roles) {
            counts.put(role.getName(), roleService.countAdminsByRole(role.getName()));
        }
        return Result.ok(counts);
    }
}
