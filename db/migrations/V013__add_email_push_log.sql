CREATE TABLE IF NOT EXISTS tu_email_push_log (
    id VARCHAR(32) PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL COMMENT '用户ID',
    push_date DATE NOT NULL COMMENT '推送日期',
    type VARCHAR(32) NOT NULL DEFAULT 'daily_recommend' COMMENT '推送类型',
    title_library_id VARCHAR(32) COMMENT '关联标题库ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, push_date),
    INDEX idx_push_date (push_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件推送日志表';
