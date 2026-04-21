package com.example.blogger.service;

import com.example.blogger.entity.Role;
import com.example.blogger.mapper.RoleMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class RoleService {
    private final RoleMapper roleMapper;

    public RoleService(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    public List<Role> list() {
        return roleMapper.findAll();
    }

    public Role getById(String id) {
        return roleMapper.findById(id);
    }

    public void save(Role role) {
        if (role.getId() == null || role.getId().isEmpty()) {
            role.setId(UUID.randomUUID().toString().replace("-", ""));
            roleMapper.insert(role);
        } else {
            roleMapper.update(role);
        }
    }

    public void delete(String id) {
        roleMapper.delete(id);
    }

    public int countAdminsByRole(String roleName) {
        return roleMapper.countAdminsByRole(roleName);
    }
}
