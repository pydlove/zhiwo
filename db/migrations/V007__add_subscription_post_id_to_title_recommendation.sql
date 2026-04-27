-- V007__add_subscription_post_id_to_title_recommendation
-- 2026-04-23 推荐记录增加订阅文章ID字段

ALTER TABLE tu_title_recommendation ADD COLUMN subscription_post_id VARCHAR(64) NULL COMMENT '关联的订阅文章ID';
CREATE INDEX idx_title_recommendation_post_id ON tu_title_recommendation(subscription_post_id);

INSERT INTO _schema_version (version, description) VALUES ('V007', 'add_subscription_post_id_to_title_recommendation');
