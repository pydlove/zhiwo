-- V009__add_invite_code_to_user
-- 2026-04-26 用户表新增邀请码和被邀请码字段

ALTER TABLE tu_user ADD COLUMN invite_code VARCHAR(20) NULL COMMENT '用户邀请码，唯一';
ALTER TABLE tu_user ADD COLUMN invited_by VARCHAR(64) NULL COMMENT '邀请人用户ID';

CREATE UNIQUE INDEX idx_user_invite_code ON tu_user(invite_code);
CREATE INDEX idx_user_invited_by ON tu_user(invited_by);

INSERT INTO _schema_version (version, description) VALUES ('V009', 'add_invite_code_to_user');

INSERT INTO _schema_version (version, description) VALUES ('V009', 'add_invite_code_to_user');
