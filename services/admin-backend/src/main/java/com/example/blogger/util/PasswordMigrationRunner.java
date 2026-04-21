package com.example.blogger.util;

import com.example.blogger.entity.Admin;
import com.example.blogger.mapper.AdminMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("migrate")
public class PasswordMigrationRunner implements ApplicationRunner {

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;

    public PasswordMigrationRunner(AdminMapper adminMapper, PasswordEncoder passwordEncoder) {
        this.adminMapper = adminMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("========================================");
        System.out.println("管理员密码批量迁移开始");
        System.out.println("========================================");

        List<Admin> admins = adminMapper.findAll();
        int migrated = 0;
        int skipped = 0;

        for (Admin admin : admins) {
            String pwd = admin.getPassword();
            if (pwd == null || pwd.isEmpty() || pwd.startsWith("$2a$")) {
                skipped++;
                continue;
            }
            String hashed = passwordEncoder.encode(pwd);
            adminMapper.updatePassword(admin.getId(), hashed);
            migrated++;
            System.out.println("已迁移: " + admin.getUsername());
        }

        System.out.println("========================================");
        System.out.println("迁移完成: 已处理 " + migrated + " 条, 跳过 " + skipped + " 条");
        System.out.println("========================================");
    }
}
