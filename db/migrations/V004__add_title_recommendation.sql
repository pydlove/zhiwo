-- V004__add_title_recommendation
-- 2026-04-22 新增标题推荐记录表

CREATE TABLE IF NOT EXISTS tu_title_recommendation (
    id VARCHAR(36) PRIMARY KEY,
    title_library_id VARCHAR(36) NOT NULL COMMENT '标题库ID',
    user_id VARCHAR(36) NOT NULL COMMENT '推荐用户ID',
    recommend_date DATE NOT NULL COMMENT '推荐日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_title_user_date (title_library_id, user_id, recommend_date)
) COMMENT='标题推荐记录';

INSERT INTO _schema_version (version, description) VALUES ('V004', 'add_title_recommendation');
