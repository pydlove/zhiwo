ALTER TABLE tu_user ADD COLUMN is_trial TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否试用用户' AFTER is_distributor;

UPDATE tu_user SET is_trial = 0 WHERE is_trial IS NULL;
