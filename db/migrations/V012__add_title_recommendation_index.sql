-- 为 tu_title_recommendation 表添加复合索引，优化标题库列表查询中"获取每个标题最新推荐记录"的性能
CREATE INDEX idx_title_recommendation_library_created ON tu_title_recommendation(title_library_id, created_at);
