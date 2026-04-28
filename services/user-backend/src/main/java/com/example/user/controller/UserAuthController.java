package com.example.user.controller;

import com.example.user.entity.MembershipPlan;
import com.example.user.entity.Result;
import com.example.user.entity.User;
import com.example.user.mapper.MembershipPlanMapper;
import com.example.user.mapper.UserMapper;
import com.example.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class UserAuthController {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MembershipPlanMapper membershipPlanMapper;
    private final UserService userService;

    public UserAuthController(UserMapper userMapper, PasswordEncoder passwordEncoder, MembershipPlanMapper membershipPlanMapper, UserService userService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.membershipPlanMapper = membershipPlanMapper;
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> req) {
        String username = req.get("username");
        String password = req.get("password");
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return Result.error("账号或密码错误");
        }

        String storedPassword = user.getPassword();
        boolean matched;
        if (storedPassword != null && storedPassword.startsWith("$2a$")) {
            matched = passwordEncoder.matches(password, storedPassword);
        } else {
            // 兼容旧明文密码：验证通过后自动升级为 BCrypt
            matched = password.equals(storedPassword);
            if (matched) {
                userMapper.updatePassword(user.getId(), passwordEncoder.encode(password));
            }
        }

        if (!matched) {
            return Result.error("账号或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            return Result.error("账号待审核，请联系客服");
        }
        userMapper.updateLastLogin(user.getId(), LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        Map<String, Object> data = new HashMap<>();
        data.put("token", "user-token-" + user.getId());
        data.put("user", user);
        if (user.getMembershipPlanId() != null && !user.getMembershipPlanId().isEmpty()) {
            MembershipPlan plan = membershipPlanMapper.findById(user.getMembershipPlanId());
            if (plan != null) {
                data.put("plan", plan);
            }
        }
        return Result.ok(data);
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> req) {
        String username = req.get("username");
        String password = req.get("password");
        String inviteCode = req.get("inviteCode");

        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            return Result.error("密码不能为空");
        }
        if (password.length() < 6) {
            return Result.error("密码长度不能少于6位");
        }

        User existing = userMapper.findByUsername(username.trim());
        if (existing != null) {
            return Result.error("用户名已被注册");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setStatus(0);
        user.setAiLimit(50);
        user.setTrackLimit(0);
        user.setPlatformLimit("");
        user.setExpireDate(LocalDate.now().plusYears(1));
        user.setCanSetEmail(0);
        user.setEmailReceive(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        if (inviteCode != null && !inviteCode.trim().isEmpty()) {
            User inviter = userMapper.findByInviteCode(inviteCode.trim());
            if (inviter != null) {
                user.setInvitedBy(inviter.getId());
            }
        }

        userService.save(user);

        Map<String, Object> data = new HashMap<>();
        data.put("token", "user-token-" + user.getId());
        data.put("user", user);
        return Result.ok(data);
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody Map<String, String> req) {
        String userId = req.get("userId");
        String oldPassword = req.get("oldPassword");
        String newPassword = req.get("newPassword");

        if (userId == null || userId.isEmpty()) {
            return Result.error("用户未登录");
        }
        if (oldPassword == null || oldPassword.isEmpty()) {
            return Result.error("请输入原密码");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return Result.error("请输入新密码");
        }
        if (newPassword.length() < 6) {
            return Result.error("新密码长度不能少于6位");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        String storedPassword = user.getPassword();
        boolean matched;
        if (storedPassword != null && storedPassword.startsWith("$2a$")) {
            matched = passwordEncoder.matches(oldPassword, storedPassword);
        } else {
            matched = oldPassword.equals(storedPassword);
        }

        if (!matched) {
            return Result.error("原密码错误");
        }

        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
        return Result.ok(null);
    }
}
