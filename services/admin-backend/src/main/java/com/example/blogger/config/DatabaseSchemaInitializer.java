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

            ensureTable(conn, "tu_title_generate_task",
                "CREATE TABLE IF NOT EXISTS tu_title_generate_task (" +
                "  id VARCHAR(32) PRIMARY KEY COMMENT '任务ID'," +
                "  status VARCHAR(20) DEFAULT 'pending' COMMENT '任务状态'," +
                "  platforms VARCHAR(500) DEFAULT NULL COMMENT '平台列表JSON'," +
                "  track_ids VARCHAR(500) DEFAULT NULL COMMENT '赛道ID列表JSON'," +
                "  count_per_combo INT DEFAULT 3 COMMENT '每个组合生成数量'," +
                "  instruction VARCHAR(1000) DEFAULT NULL COMMENT '生成方向'," +
                "  result_file_url VARCHAR(255) DEFAULT NULL COMMENT '结果Excel文件URL'," +
                "  result_file_name VARCHAR(255) DEFAULT NULL COMMENT '结果Excel文件名'," +
                "  error_message VARCHAR(500) DEFAULT NULL COMMENT '失败原因'," +
                "  progress_step INT DEFAULT 0 COMMENT '进度步骤'," +
                "  progress_message VARCHAR(200) DEFAULT '' COMMENT '当前进度描述'," +
                "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "  processed_at DATETIME DEFAULT NULL COMMENT '完成时间'" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V2标题生成任务表'");
        } catch (Exception e) {
            log.error("[DatabaseSchemaInitializer] 数据库连接失败: {}", e.getMessage(), e);
        }
    }

    private void ensureTable(Connection conn, String tableName, String createSql) {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            boolean exists = false;
            String catalog = conn.getCatalog();
            try (ResultSet rs = metaData.getTables(catalog, null, tableName, null)) {
                exists = rs.next();
            }
            if (!exists) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(createSql);
                    log.info("[DatabaseSchemaInitializer] 已自动创建表: {}", tableName);
                }
                // 创建索引
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("CREATE INDEX idx_tg_task_status ON " + tableName + "(status)");
                    stmt.executeUpdate("CREATE INDEX idx_tg_task_created_at ON " + tableName + "(created_at)");
                }
            } else {
                log.debug("[DatabaseSchemaInitializer] 表 {} 已存在，跳过创建", tableName);
            }
        } catch (Exception e) {
            log.error("[DatabaseSchemaInitializer] 创建表 {} 失败: {}", tableName, e.getMessage());
        }
    }

    private void ensureColumn(Connection conn, String tableName, String columnName, String alterSql) {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            boolean exists = false;
            String catalog = conn.getCatalog();
            try (ResultSet rs = metaData.getColumns(catalog, null, tableName, columnName)) {
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
