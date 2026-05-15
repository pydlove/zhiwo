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

            ensureColumn(conn, "tu_title_library", "task_id",
                "ALTER TABLE tu_title_library ADD COLUMN task_id VARCHAR(32) DEFAULT NULL COMMENT '关联的生成任务ID'");

            ensureColumn(conn, "tu_user", "theme_color",
                "ALTER TABLE tu_user ADD COLUMN theme_color VARCHAR(20) DEFAULT '#fa541c' COMMENT '文章主题色'");

            ensureColumn(conn, "tu_user", "title_font_size",
                "ALTER TABLE tu_user ADD COLUMN title_font_size INT DEFAULT 16 COMMENT '文章标题字号(pt)'");

            ensureColumn(conn, "tu_user", "content_font_size",
                "ALTER TABLE tu_user ADD COLUMN content_font_size INT DEFAULT 12 COMMENT '文章正文字号(pt)'");

            ensureColumn(conn, "tu_title_library", "is_confirmed",
                "ALTER TABLE tu_title_library ADD COLUMN is_confirmed INT DEFAULT 0 COMMENT '是否确认: 0=未确认 1=已确认'");

            ensureColumn(conn, "tu_title_library", "is_ai_flavor_heavy",
                "ALTER TABLE tu_title_library ADD COLUMN is_ai_flavor_heavy INT DEFAULT 0 COMMENT 'AI味重标记: 0=正常 1=AI味重'");

            ensureColumn(conn, "tu_title_library", "ai_flavor_status",
                "ALTER TABLE tu_title_library ADD COLUMN ai_flavor_status INT DEFAULT 0 COMMENT 'AI味状态: 0/null=未检测 1=已通过 2=AI味重'");

            // 迁移 is_ai_passed / is_ai_flavor_heavy 数据到 ai_flavor_status
            migrateAiFlavorStatus(conn);

            ensureColumn(conn, "tu_title_generation_task", "process_started_at",
                "ALTER TABLE tu_title_generation_task ADD COLUMN process_started_at DATETIME DEFAULT NULL COMMENT '开始生成时间（进入processing状态的时间）'");

            ensureColumn(conn, "tu_title_library", "confirm_status",
                "ALTER TABLE tu_title_library ADD COLUMN confirm_status INT DEFAULT 0 COMMENT '确认状态: 0=未确认 1=已确认 2=已拒绝'");

            // 迁移 is_confirmed 数据到 confirm_status
            migrateConfirmStatus(conn);

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

            ensureTable(conn, "tu_llm_config",
                "CREATE TABLE IF NOT EXISTS tu_llm_config (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  provider VARCHAR(20) NOT NULL COMMENT '提供商: kimi/minimax'," +
                "  api_key VARCHAR(255) DEFAULT NULL COMMENT 'API Key'," +
                "  model VARCHAR(50) DEFAULT NULL COMMENT '模型名称'," +
                "  is_active TINYINT DEFAULT 0 COMMENT '是否当前选中: 0=否 1=是'," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "  UNIQUE KEY uk_provider (provider)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大模型配置表'");

            ensureTable(conn, "tu_prompt_template",
                "CREATE TABLE IF NOT EXISTS tu_prompt_template (" +
                "  id VARCHAR(32) PRIMARY KEY COMMENT '模板ID'," +
                "  name VARCHAR(100) NOT NULL COMMENT '模板名称'," +
                "  content LONGTEXT NOT NULL COMMENT '提示词内容'," +
                "  type VARCHAR(50) DEFAULT 'generate_title' COMMENT '类别: generate_title=生成标题'," +
                "  is_default TINYINT DEFAULT 0 COMMENT '是否默认: 0=否 1=是'," +
                "  is_deleted TINYINT DEFAULT 0 COMMENT '是否删除: 0=否 1=是'," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提示词模板表'");

            ensureTable(conn, "tu_image_library",
                "CREATE TABLE IF NOT EXISTS tu_image_library (" +
                "  id VARCHAR(32) PRIMARY KEY COMMENT '图片ID'," +
                "  name VARCHAR(255) DEFAULT NULL COMMENT '原始文件名'," +
                "  url VARCHAR(500) NOT NULL COMMENT '图片访问URL'," +
                "  categories VARCHAR(500) DEFAULT NULL COMMENT '赛道ID列表JSON'," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片库表'");

            ensureTable(conn, "tu_announcement",
                "CREATE TABLE IF NOT EXISTS tu_announcement (" +
                "  id VARCHAR(32) PRIMARY KEY COMMENT '公告ID'," +
                "  type VARCHAR(50) NOT NULL COMMENT '公告类型: article_push=文章推送公告'," +
                "  content TEXT NOT NULL COMMENT '公告内容（支持HTML）'," +
                "  is_enabled INT NOT NULL DEFAULT 1 COMMENT '是否开启: 0=关闭 1=开启'," +
                "  is_deleted INT NOT NULL DEFAULT 0 COMMENT '是否删除: 0=否 1=是'," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "  UNIQUE KEY uk_type (type)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告管理表'");

            ensureColumn(conn, "tu_title_generate_task", "duplicate_count",
                "ALTER TABLE tu_title_generate_task ADD COLUMN duplicate_count INT DEFAULT 0 COMMENT '重复标题数量'");

            ensureColumn(conn, "tu_title_generate_task", "inserted_count",
                "ALTER TABLE tu_title_generate_task ADD COLUMN inserted_count INT DEFAULT 0 COMMENT '成功插入标题数量'");

            ensureColumn(conn, "tu_image_library", "updated_at",
                "ALTER TABLE tu_image_library ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
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

    private void migrateConfirmStatus(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "UPDATE tu_title_library SET confirm_status = 1 WHERE is_confirmed = 1 AND (confirm_status IS NULL OR confirm_status = 0)"
            );
            log.info("[DatabaseSchemaInitializer] 已迁移 is_confirmed 数据到 confirm_status");
        } catch (Exception e) {
            log.error("[DatabaseSchemaInitializer] 迁移 confirm_status 失败: {}", e.getMessage());
        }
    }

    private void migrateAiFlavorStatus(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "UPDATE tu_title_library SET ai_flavor_status = 2 WHERE is_ai_flavor_heavy = 1 AND ai_flavor_status IS NULL"
            );
            stmt.executeUpdate(
                "UPDATE tu_title_library SET ai_flavor_status = 1 WHERE is_ai_passed = 1 AND (is_ai_flavor_heavy IS NULL OR is_ai_flavor_heavy != 1) AND ai_flavor_status IS NULL"
            );
            log.info("[DatabaseSchemaInitializer] 已迁移 AI味状态数据到 ai_flavor_status");
        } catch (Exception e) {
            log.error("[DatabaseSchemaInitializer] 迁移 ai_flavor_status 失败: {}", e.getMessage());
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
