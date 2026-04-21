-- V001__add_email_columns
-- 2026-04-21 给 tu_user 增加邮件接收相关字段

ALTER TABLE tu_user
    ADD COLUMN IF NOT EXISTS can_set_email TINYINT DEFAULT 0 COMMENT '0=不允许, 1=允许设置邮箱接收文章',
    ADD COLUMN IF NOT EXISTS email_receive TINYINT DEFAULT 0 COMMENT '0=不接收, 1=接收邮件推送';

INSERT INTO _schema_version (version, description) VALUES ('V001', 'add_email_columns');
