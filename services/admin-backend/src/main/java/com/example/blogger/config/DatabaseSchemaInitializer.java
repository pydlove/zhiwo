package com.example.blogger.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class DatabaseSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaInitializer.class);

    private final DataSource dataSource;

    public DatabaseSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            ensureColumn(conn, "tu_title_generation_task", "progress_step",
                "ALTER TABLE tu_title_generation_task ADD COLUMN progress_step INT DEFAULT 0 COMMENT '进度步骤：0=排队 1=构建提示词 2=大模型生成 3=写入文件 4=完成'");

            ensureColumn(conn, "tu_title_generation_task", "progress_message",
                "ALTER TABLE tu_title_generation_task ADD COLUMN progress_message VARCHAR(200) DEFAULT '' COMMENT '当前进度描述'");

            ensureColumn(conn, "tu_title_library", "generate_status",
                "ALTER TABLE tu_title_library ADD COLUMN generate_status INT DEFAULT 0 COMMENT '生成状态：0=未生成 1=生成成功 2=生成中'");

            ensureColumn(conn, "tu_title_generation_task", "generated_content",
                "ALTER TABLE tu_title_generation_task ADD COLUMN generated_content LONGTEXT COMMENT '大模型生成的原始内容'");

            ensureColumn(conn, "tu_user", "theme_color",
                "ALTER TABLE tu_user ADD COLUMN theme_color VARCHAR(20) DEFAULT '#fa541c' COMMENT '文章主题色'");
        } catch (Exception e) {
            log.error("[DatabaseSchemaInitializer] 数据库连接失败: {}", e.getMessage(), e);
        }
    }

    private void ensureColumn(Connection conn, String tableName, String columnName, String alterSql) {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            boolean exists = false;
            try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
                exists = rs.next();
            }
            if (!exists) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(alterSql);
                    log.info("[DatabaseSchemaInitializer] 已自动添加字段: {}.{}", tableName, columnName);
                }
            } else {
                log.debug("[DatabaseSchemaInitializer] {}.{} 已存在，跳过迁移", tableName, columnName);
            }
        } catch (Exception e) {
            log.error("[DatabaseSchemaInitializer] 添加字段 {}.{} 失败: {}", tableName, columnName, e.getMessage());
        }
    }
}
