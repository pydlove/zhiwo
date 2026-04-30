-- V019__add_is_used_to_title_library
-- 2026-04-29 标题库新增使用状态字段

ALTER TABLE tu_title_library ADD COLUMN is_used INT DEFAULT 0 COMMENT '是否已使用：0-未使用，1-已使用' AFTER use_count;

INSERT INTO _schema_version (version, description) VALUES ('V019', 'add_is_used_to_title_library');
