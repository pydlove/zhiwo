-- V006__add_description_to_title_library
-- 2026-04-23 标题库新增描述字段

ALTER TABLE tu_title_library ADD COLUMN description VARCHAR(500) COMMENT '标题SEO描述' AFTER title;

INSERT INTO _schema_version (version, description) VALUES ('V006', 'add_description_to_title_library');
