-- 订单表添加退单金额字段
ALTER TABLE tu_order ADD COLUMN refund_amount DECIMAL(10, 2) DEFAULT 0 COMMENT '退单金额' AFTER amount;
