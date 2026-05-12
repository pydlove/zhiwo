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
            DatabaseMetaData metaData = conn.getMetaData();

            // 检查 progress_step 列是否存在
            boolean hasProgressStep = false;
            boolean hasProgressMessage = false;

            try (ResultSet rs = metaData.getColumns(null, null, "tu_title_generation_task", "progress_step")) {
                hasProgressStep = rs.next();
            }
            try (ResultSet rs = metaData.getColumns(null, null, "tu_title_generation_task", "progress_message")) {
                hasProgressMessage = rs.next();
            }

            if (!hasProgressStep || !hasProgressMessage) {
                try (Statement stmt = conn.createStatement()) {
                    if (!hasProgressStep) {
                        stmt.executeUpdate(
                            "ALTER TABLE tu_title_generation_task ADD COLUMN progress_step INT DEFAULT 0 COMMENT '进度步骤：0=排队 1=构建提示词 2=大模型生成 3=写入文件 4=完成'"
                        );
                        log.info("[DatabaseSchemaInitializer] 已自动添加字段: progress_step");
                    }
                    if (!hasProgressMessage) {
                        stmt.executeUpdate(
                            "ALTER TABLE tu_title_generation_task ADD COLUMN progress_message VARCHAR(200) DEFAULT '' COMMENT '当前进度描述'"
                        );
                        log.info("[DatabaseSchemaInitializer] 已自动添加字段: progress_message");
                    }
                }
            } else {
                log.debug("[DatabaseSchemaInitializer] tu_title_generation_task 表字段已存在，跳过迁移");
            }

            // 检查 tu_title_library 表是否缺少 generate_status 字段
            boolean hasGenerateStatus = false;
            try (ResultSet rs = metaData.getColumns(null, null, "tu_title_library", "generate_status")) {
                hasGenerateStatus = rs.next();
            }
            if (!hasGenerateStatus) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                        "ALTER TABLE tu_title_library ADD COLUMN generate_status INT DEFAULT 0 COMMENT '生成状态：0=未生成 1=生成成功 2=生成中'"
                    );
                    log.info("[DatabaseSchemaInitializer] 已自动添加字段: generate_status");
                }
            }
        } catch (Exception e) {
            log.error("[DatabaseSchemaInitializer] 数据库字段迁移失败: {}", e.getMessage(), e);
        }
    }
}
