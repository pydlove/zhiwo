package com.example.blogger.service;

import com.example.blogger.entity.User;
import com.example.blogger.mapper.UserMapper;
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
            userMapper.insert(user);
        } else {
            userMapper.update(user);
        }
    }

    public void delete(String id) {
        userMapper.delete(id);
    }
}
