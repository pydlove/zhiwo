-- V003__add_title_library
-- 2026-04-22 新增标题库表

CREATE TABLE IF NOT EXISTS tu_title_library (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(500) NOT NULL COMMENT '标题内容',
    platform VARCHAR(50) COMMENT '适用平台，如：公众号、今日头条、百家号',
    track_id VARCHAR(36) COMMENT '关联赛道ID',
    use_count INT DEFAULT 0 COMMENT '使用次数',
    is_deleted INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='标题库';

INSERT INTO _schema_version (version, description) VALUES ('V003', 'add_title_library');
