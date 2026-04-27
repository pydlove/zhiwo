-- V009__add_is_hot_to_track
-- 2026-04-24 给赛道表增加热门标记字段

ALTER TABLE tu_track ADD COLUMN is_hot TINYINT DEFAULT 0 COMMENT '0=普通, 1=热门';

INSERT INTO _schema_version (version, description) VALUES ('V009', 'add_is_hot_to_track');
