-- V021__add_order
-- 2026-04-30 新增订单/收益管理表

CREATE TABLE IF NOT EXISTS tu_order (
    id VARCHAR(32) PRIMARY KEY,
    user_id VARCHAR(32) COMMENT '关联用户ID',
    user_name VARCHAR(100) COMMENT '用户名称',
    plan_id VARCHAR(32) COMMENT '关联套餐ID',
    plan_name VARCHAR(100) COMMENT '套餐名称',
    type VARCHAR(20) COMMENT '类型：open_account开户/renew续费/upgrade升级',
    amount DECIMAL(10,2) DEFAULT 0 COMMENT '订单金额',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='订单表';

INSERT INTO _schema_version (version, description) VALUES ('V021', 'add_order');
