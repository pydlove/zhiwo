package com.example.blogger.service;

import com.example.blogger.entity.User;
import com.example.blogger.mapper.UserMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @PostConstruct
    public void initInviteCodes() {
        List<User> users = userMapper.findAll();
        int generated = 0;
        for (User user : users) {
            if (user.getInviteCode() == null || user.getInviteCode().isEmpty()) {
                String code;
                do {
                    code = generateInviteCode();
                } while (userMapper.findByInviteCode(code) != null);
                userMapper.updateInviteCode(user.getId(), code);
                generated++;
            }
        }
        if (generated > 0) {
            System.out.println("[InviteCodeInit] 已为 " + generated + " 位存量用户生成邀请码");
        }
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
            if (user.getIsReal() == null) user.setIsReal(1);
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
        // ensure uniqueness
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
