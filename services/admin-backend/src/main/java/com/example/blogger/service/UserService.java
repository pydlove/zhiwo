package com.example.blogger.service;

import com.example.blogger.entity.User;
import com.example.blogger.mapper.UserMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final DataSource dataSource;

    public UserService(UserMapper userMapper, DataSource dataSource) {
        this.userMapper = userMapper;
        this.dataSource = dataSource;
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

    /* TODO: 临时逻辑 —— 启动时迁移旧的用户类型字段数据到 user_type，下次发布前请注释掉下面整个方法 */
    @PostConstruct
    public void initMigrateUserType() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            boolean hasOldColumns = false;
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, "tu_user", "is_distributor")) {
                if (rs.next()) hasOldColumns = true;
            }
            if (!hasOldColumns) {
                System.out.println("[临时迁移user_type] 旧字段已不存在，跳过迁移");
                return;
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("UPDATE tu_user SET user_type = 2 WHERE is_distributor = 1");
                stmt.executeUpdate("UPDATE tu_user SET user_type = 1 WHERE is_account_opened = 1 AND user_type != 2");
                stmt.executeUpdate("UPDATE tu_user SET user_type = 3 WHERE is_trial = 1 AND user_type NOT IN (1, 2)");
                System.out.println("[临时迁移user_type] 迁移完成");
            }
        } catch (Exception e) {
            System.out.println("[临时迁移user_type] 迁移失败: " + e.getMessage());
        }
    }
    /* TODO: 临时逻辑结束 —— 下次请注释掉上面整个方法 */

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
            if (user.getUserType() == null) user.setUserType(1);
            if (user.getAiLimit() == null) user.setAiLimit(0);
            if (user.getTrackLimit() == null) user.setTrackLimit(0);
            if (user.getCanSetEmail() == null) user.setCanSetEmail(0);
            if (user.getEmailReceive() == null) user.setEmailReceive(0);
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

    public void batchUpdateAdminId(List<String> userIds, String adminId) {
        userMapper.batchUpdateAdminId(userIds, adminId);
    }

    public List<Map<String, Object>> findExpiringUsers(int days) {
        return userMapper.findExpiringUsers(days);
    }
}
