-- V020__add_banned_word
-- 2026-04-30 新增违禁词管理表

CREATE TABLE IF NOT EXISTS tu_banned_word (
    id VARCHAR(32) PRIMARY KEY,
    word VARCHAR(100) NOT NULL COMMENT '违禁词',
    replacement VARCHAR(200) COMMENT '替换词，为空则直接删除/避免',
    category VARCHAR(50) COMMENT '分类：极限词、医疗词、金融词、诱导词、政治敏感',
    severity VARCHAR(20) DEFAULT 'block' COMMENT '等级：block严禁, caution慎用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='违禁词库';

INSERT INTO tu_banned_word (id, word, replacement, category, severity) VALUES
('b001', '最好', '很好', '极限词', 'block'),
('b002', '最佳', '很出色', '极限词', 'block'),
('b003', '第一', '领先', '极限词', 'block'),
('b004', '唯一', '少有的', '极限词', 'block'),
('b005', '顶级', '优质', '极限词', 'block'),
('b006', '最高级', '更高级', '极限词', 'block'),
('b007', '万能', '多用途', '极限词', 'block'),
('b008', '100%', '绝大多数', '极限词', 'caution'),
('b009', '绝对', '相当', '极限词', 'caution'),
('b010', '永久', '长期', '极限词', 'caution'),
('b011', '疗效', '效果', '医疗词', 'block'),
('b012', '治愈', '改善', '医疗词', 'block'),
('b013', '根治', '从根本上改善', '医疗词', 'block'),
('b014', '无副作用', '成分温和', '医疗词', 'block'),
('b015', '抗衰老', '延缓衰老', '医疗词', 'caution'),
('b016', '祛斑', '淡化斑点', '医疗词', 'caution'),
('b017', '药方', '调理方法', '医疗词', 'caution'),
('b018', '偏方', '传统方法', '医疗词', 'caution'),
('b019', '保本', '稳健型', '金融词', 'block'),
('b020', '稳赚', '收益相对稳定', '金融词', 'block'),
('b021', '零风险', '风险可控', '金融词', 'block'),
('b022', '高收益', '预期收益较好', '金融词', 'caution'),
('b023', '翻倍', '大幅增长', '金融词', 'caution'),
('b024', '财富自由', '财务宽裕', '金融词', 'caution'),
('b025', '跳楼价', '超值价', '诱导词', 'block'),
('b026', '亏本卖', '薄利多销', '诱导词', 'block'),
('b027', '错过再等一年', '限时优惠', '诱导词', 'caution'),
('b028', '震惊', '令人关注', '诱导词', 'caution'),
('b029', '国家级', '省级以上', '极限词', 'block'),
('b030', '首家', '率先', '极限词', 'block'),
('b031', '独家', '特有', '极限词', 'block'),
('b032', '全网最低', '价格优惠', '极限词', 'block'),
('b033', '最便宜', '性价比高', '极限词', 'block'),
('b034', '最强', '很强', '极限词', 'block'),
('b035', '首选', '优选', '极限词', 'block');

INSERT INTO _schema_version (version, description) VALUES ('V020', 'add_banned_word');
