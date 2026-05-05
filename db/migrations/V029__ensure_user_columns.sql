-- Ensure user columns exist for open account feature
-- V029__ensure_user_columns

-- Add wx_name if not exists
SET @exists = (SELECT COUNT(*) FROM information_schema.columns 
    WHERE table_schema = 'blogger_db' AND table_name = 'tu_user' AND column_name = 'wx_name');
SET @sql = IF(@exists = 0, 'ALTER TABLE tu_user ADD COLUMN wx_name VARCHAR(100) COMMENT "公众号名称" AFTER wx_id', 'SELECT "wx_name already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add nick_name if not exists
SET @exists = (SELECT COUNT(*) FROM information_schema.columns 
    WHERE table_schema = 'blogger_db' AND table_name = 'tu_user' AND column_name = 'nick_name');
SET @sql = IF(@exists = 0, 'ALTER TABLE tu_user ADD COLUMN nick_name VARCHAR(100) COMMENT "微信名称/昵称" AFTER wx_name', 'SELECT "nick_name already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add user_type if not exists
SET @exists = (SELECT COUNT(*) FROM information_schema.columns 
    WHERE table_schema = 'blogger_db' AND table_name = 'tu_user' AND column_name = 'user_type');
SET @sql = IF(@exists = 0, 'ALTER TABLE tu_user ADD COLUMN user_type TINYINT NOT NULL DEFAULT 1 COMMENT "用户类型 1-开户 2-分成 3-试用" AFTER status', 'SELECT "user_type already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO _schema_version (version, description) VALUES ('V029', 'ensure_user_columns_for_open_account');
