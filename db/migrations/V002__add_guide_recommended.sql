-- V002__add_guide_recommended
-- 2026-04-21 给 tu_guide 增加是否推荐字段

ALTER TABLE tu_guide
    ADD COLUMN is_recommended TINYINT DEFAULT 0 COMMENT '0=不推荐, 1=推荐';

INSERT INTO _schema_version (version, description) VALUES ('V002', 'add_guide_recommended');
