-- V022__add_nick_name_to_user
-- 2026-04-30 用户表新增微信名称（昵称）字段

ALTER TABLE tu_user ADD COLUMN nick_name VARCHAR(100) COMMENT '微信名称/昵称' AFTER wx_name;

INSERT INTO _schema_version (version, description) VALUES ('V022', 'add_nick_name_to_user');
