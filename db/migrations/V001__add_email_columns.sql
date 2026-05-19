-- V001__add_email_columns
-- 2026-04-21 给 tu_user 增加邮件接收相关字段

DROP PROCEDURE IF EXISTS v001_add_email_columns;

DELIMITER //

CREATE PROCEDURE v001_add_email_columns()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'tu_user'
        AND COLUMN_NAME = 'can_set_email'
    ) THEN
        ALTER TABLE tu_user ADD COLUMN can_set_email TINYINT DEFAULT 0 COMMENT '0=不允许, 1=允许设置邮箱接收文章';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'tu_user'
        AND COLUMN_NAME = 'email_receive'
    ) THEN
        ALTER TABLE tu_user ADD COLUMN email_receive TINYINT DEFAULT 0 COMMENT '0=不接收, 1=接收邮件推送';
    END IF;
END //

DELIMITER ;

CALL v001_add_email_columns();
DROP PROCEDURE IF EXISTS v001_add_email_columns;

INSERT INTO _schema_version (version, description) VALUES ('V001', 'add_email_columns');
