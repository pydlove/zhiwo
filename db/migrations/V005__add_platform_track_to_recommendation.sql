-- V005__add_platform_track_to_recommendation
-- 2026-04-23 推荐记录增加平台和赛道字段，用于每日去重

ALTER TABLE tu_title_recommendation
    ADD COLUMN platform VARCHAR(50) COMMENT '平台' AFTER user_id,
    ADD COLUMN track_id VARCHAR(36) COMMENT '赛道ID' AFTER platform;

INSERT INTO _schema_version (version, description) VALUES ('V005', 'add_platform_track_to_recommendation');
