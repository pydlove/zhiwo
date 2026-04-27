package com.example.user.service;

import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public List<User> list() {
        return userMapper.findAll();
    }

    public User getById(String id) {
        return userMapper.findById(id);
    }

    public void save(User user) {
        if (user.getId() == null || user.getId().isEmpty()) {
            user.setId(UUID.randomUUID().toString().replace("-", ""));
            if (user.getStatus() == null) user.setStatus(1);
            if (user.getInviteCode() == null || user.getInviteCode().isEmpty()) {
                user.setInviteCode(generateInviteCode());
            }
            userMapper.insert(user);
        } else {
            userMapper.update(user);
        }
    }

    private String generateInviteCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        String code = sb.toString();
        if (userMapper.findByInviteCode(code) != null) {
            return generateInviteCode();
        }
        return code;
    }

    public User getByInviteCode(String inviteCode) {
        return userMapper.findByInviteCode(inviteCode);
    }

    public void delete(String id) {
        userMapper.delete(id);
    }
}
