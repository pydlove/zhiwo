CREATE TABLE tu_email_push_log (
    id VARCHAR(32) PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL,
    push_date DATE NOT NULL,
    type VARCHAR(32) NOT NULL DEFAULT 'daily_recommend',
    title_library_id VARCHAR(32),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, push_date),
    INDEX idx_push_date (push_date)
);
