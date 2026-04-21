package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.User;
import com.example.blogger.entity.UserTrack;
import com.example.blogger.service.UserService;
import com.example.blogger.service.UserTrackService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;
    private final UserTrackService userTrackService;

    public UserController(UserService userService, UserTrackService userTrackService) {
        this.userService = userService;
        this.userTrackService = userTrackService;
    }

    @GetMapping
    public Result<List<User>> list() {
        List<User> users = userService.list();
        for (User u : users) {
            List<UserTrack> tracks = userTrackService.listByUser(u.getId());
            u.setTrackIds(tracks.stream().map(UserTrack::getTrackId).collect(Collectors.toList()));
        }
        return Result.ok(users);
    }

    @GetMapping("/{id}")
    public Result<User> get(@PathVariable String id) {
        return Result.ok(userService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody User user) {
        userService.save(user);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody User user) {
        User existing = userService.getById(id);
        if (existing == null) {
            return Result.error("用户不存在");
        }
        if (user.getUsername() != null) existing.setUsername(user.getUsername());
        if (user.getPassword() != null) existing.setPassword(user.getPassword());
        if (user.getStatus() != null) existing.setStatus(user.getStatus());
        if (user.getPhone() != null) existing.setPhone(user.getPhone());
        if (user.getEmail() != null) existing.setEmail(user.getEmail());
        if (user.getWxId() != null) existing.setWxId(user.getWxId());
        if (user.getAiLimit() != null) existing.setAiLimit(user.getAiLimit());
        if (user.getTrackLimit() != null) existing.setTrackLimit(user.getTrackLimit());
        if (user.getPlatformLimit() != null) existing.setPlatformLimit(user.getPlatformLimit());
        if (user.getAvatar() != null) existing.setAvatar(user.getAvatar());
        if (user.getTemplate() != null) existing.setTemplate(user.getTemplate());
        if (user.getExpireDate() != null) existing.setExpireDate(user.getExpireDate());
        if (user.getRemark() != null) existing.setRemark(user.getRemark());
        userService.save(existing);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        userService.delete(id);
        return Result.ok(null);
    }
}
