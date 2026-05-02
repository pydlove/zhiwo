-- 标题审核记录表
CREATE TABLE IF NOT EXISTS tu_title_review (
    id VARCHAR(36) PRIMARY KEY,
    title_library_id VARCHAR(36) NOT NULL COMMENT '标题库ID',
    review_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '审核状态: pending-待审核, approved-已通过, rejected-已拒绝',
    review_reason VARCHAR(500) NULL COMMENT '审核备注/拒绝原因',
    reviewed_by VARCHAR(36) NULL COMMENT '审核人ID',
    reviewed_at DATETIME NULL COMMENT '审核时间',
    push_status VARCHAR(20) DEFAULT 'unpushed' COMMENT '推送状态: unpushed-未推送, pushed-已推送',
    pushed_at DATETIME NULL COMMENT '推送时间',
    source VARCHAR(50) DEFAULT 'ai_generated' COMMENT '来源: ai_generated-AI生成, manual-手动录入, imported-导入',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_title_library_id (title_library_id),
    INDEX idx_review_status (review_status),
    INDEX idx_push_status (push_status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标题审核记录表';

-- 服务器配置表
CREATE TABLE IF NOT EXISTS tu_server_config (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '配置名称',
    host VARCHAR(255) NOT NULL COMMENT '服务器地址',
    port INT NOT NULL COMMENT '端口',
    is_active INT DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
    is_default INT DEFAULT 0 COMMENT '是否默认: 0-否, 1-是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='目标服务器配置';

-- 标题推送日志表
CREATE TABLE IF NOT EXISTS tu_title_push_log (
    id VARCHAR(36) PRIMARY KEY,
    title_library_id VARCHAR(36) NOT NULL COMMENT '标题库ID',
    server_config_id VARCHAR(36) NOT NULL COMMENT '目标服务器配置ID',
    title VARCHAR(500) NOT NULL COMMENT '推送的标题内容',
    platform VARCHAR(50) COMMENT '平台',
    track_id VARCHAR(36) COMMENT '赛道ID',
    status VARCHAR(20) NOT NULL COMMENT '推送状态: success-成功, failed-失败',
    error_msg TEXT NULL COMMENT '失败原因',
    pushed_by VARCHAR(36) COMMENT '推送人',
    pushed_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '推送时间',
    INDEX idx_title_library_id (title_library_id),
    INDEX idx_server_config_id (server_config_id),
    INDEX idx_status (status),
    INDEX idx_pushed_at (pushed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标题推送日志';
