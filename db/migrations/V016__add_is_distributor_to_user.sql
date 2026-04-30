ALTER TABLE tu_user ADD COLUMN is_distributor TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否分成用户：0-否，1-是';
