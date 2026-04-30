ALTER TABLE tu_user ADD COLUMN is_account_opened TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否开户用户' AFTER is_trial;
UPDATE tu_user SET is_account_opened = 1 WHERE status = 1 AND is_deleted = 0;
