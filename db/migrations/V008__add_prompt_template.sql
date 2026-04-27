-- V008__add_prompt_template
-- 2026-04-23 新增提示词模板表

CREATE TABLE IF NOT EXISTS tu_prompt_template (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    content TEXT NOT NULL COMMENT '提示词内容',
    type VARCHAR(50) DEFAULT 'generate_post' COMMENT '类型',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提示词模板表';

INSERT INTO _schema_version (version, description) VALUES ('V008', 'add_prompt_template');
