package com.example.user.util;

import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("migrate")
public class PasswordMigrationRunner implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public PasswordMigrationRunner(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("========================================");
        System.out.println("用户密码批量迁移开始");
        System.out.println("========================================");

        List<User> users = userMapper.findAll();
        int migrated = 0;
        int skipped = 0;

        for (User user : users) {
            String pwd = user.getPassword();
            if (pwd == null || pwd.isEmpty() || pwd.startsWith("$2a$")) {
                skipped++;
                continue;
            }
            String hashed = passwordEncoder.encode(pwd);
            userMapper.updatePassword(user.getId(), hashed);
            migrated++;
            System.out.println("已迁移: " + user.getUsername());
        }

        System.out.println("========================================");
        System.out.println("迁移完成: 已处理 " + migrated + " 条, 跳过 " + skipped + " 条");
        System.out.println("========================================");
    }
}
