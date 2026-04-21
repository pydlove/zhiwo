package com.example.blogger.service;

import com.example.blogger.entity.Admin;
import com.example.blogger.mapper.AdminMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class AdminService {
    private final AdminMapper adminMapper;

    public AdminService(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    public List<Admin> list() {
        return adminMapper.findAll();
    }

    public Admin getById(String id) {
        return adminMapper.findById(id);
    }

    public void save(Admin admin) {
        if (admin.getId() == null || admin.getId().isEmpty()) {
            admin.setId(UUID.randomUUID().toString().replace("-", ""));
            if (admin.getStatus() == null) admin.setStatus(1);
            adminMapper.insert(admin);
        } else {
            adminMapper.update(admin);
        }
    }

    public void delete(String id) {
        adminMapper.delete(id);
    }
}
