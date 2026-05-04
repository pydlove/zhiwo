-- 用户类型字段重构：将 is_real/is_distributor/is_trial/is_account_opened 合并为 user_type

-- 1. 新增 user_type 字段
ALTER TABLE tu_user ADD COLUMN user_type TINYINT NOT NULL DEFAULT 1 COMMENT '用户类型 1-开户 2-分成 3-试用' AFTER status;

-- 2. 数据迁移（优先级：分成(2) > 开户(1) > 试用(3)）
UPDATE tu_user SET user_type = 2 WHERE is_distributor = 1;
UPDATE tu_user SET user_type = 1 WHERE is_account_opened = 1 AND user_type != 2;
UPDATE tu_user SET user_type = 3 WHERE is_trial = 1 AND user_type NOT IN (1, 2);

-- 3. 删除旧字段
ALTER TABLE tu_user DROP COLUMN is_real;
ALTER TABLE tu_user DROP COLUMN is_distributor;
ALTER TABLE tu_user DROP COLUMN is_trial;
ALTER TABLE tu_user DROP COLUMN is_account_opened;
