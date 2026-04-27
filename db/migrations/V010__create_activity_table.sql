-- V010__create_activity_table
-- 2026-04-26 创建活动管理表

CREATE TABLE IF NOT EXISTS tu_activity (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '活动标题',
    content TEXT COMMENT '活动内容（支持HTML）',
    qr_code_url VARCHAR(500) COMMENT '二维码图片URL',
    status TINYINT DEFAULT 1 COMMENT '状态：0=下架，1=上架',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除：0=否，1=是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动管理表';

INSERT INTO _schema_version (version, description) VALUES ('V010', 'create_activity_table');
