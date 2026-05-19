-- AI Agent 自动流水线配置表
CREATE TABLE IF NOT EXISTS tu_agent_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    enabled TINYINT DEFAULT 0 COMMENT '是否启用: 0=禁用, 1=启用',
    cron_expr VARCHAR(50) DEFAULT '0 0 6 * * ?' COMMENT '定时表达式',
    similarity_threshold DECIMAL(3,2) DEFAULT 0.15 COMMENT '标题相似度阈值(0-1)',
    homogeneity_threshold DECIMAL(3,2) DEFAULT 0.15 COMMENT '同质化阈值(0-1)',
    min_titles_per_track INT DEFAULT 5 COMMENT '每赛道最少推荐数',
    history_days INT DEFAULT 30 COMMENT '历史标题取近N天',
    candidate_limit INT DEFAULT 50 COMMENT '候选标题最多取N条',
    llm_model VARCHAR(50) DEFAULT 'kimi' COMMENT '选标题用的模型',
    max_generation_concurrency INT DEFAULT 3 COMMENT '文章生成并发数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Agent 自动流水线配置';

-- 默认插入一条配置
INSERT INTO tu_agent_config (id, enabled, cron_expr, similarity_threshold, homogeneity_threshold, min_titles_per_track, history_days, candidate_limit, llm_model, max_generation_concurrency)
VALUES (1, 0, '0 0 6 * * ?', 0.15, 0.15, 5, 30, 50, 'kimi', 3)
ON DUPLICATE KEY UPDATE id=id;

-- AI Agent 执行记录表
CREATE TABLE IF NOT EXISTS tu_agent_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    execution_date DATE NOT NULL COMMENT '执行日期',
    status VARCHAR(20) DEFAULT 'running' COMMENT '状态: running/completed/failed/partial',
    total_users INT DEFAULT 0 COMMENT '总处理用户数',
    total_tracks INT DEFAULT 0 COMMENT '总处理赛道数',
    matched_titles INT DEFAULT 0 COMMENT '匹配标题数',
    generated_titles INT DEFAULT 0 COMMENT '生成标题数',
    article_tasks INT DEFAULT 0 COMMENT '文章任务数',
    failed_count INT DEFAULT 0 COMMENT '失败数',
    detail_json TEXT COMMENT '执行详情JSON',
    started_at DATETIME COMMENT '开始时间',
    completed_at DATETIME COMMENT '完成时间',
    error_message TEXT COMMENT '错误信息',
    INDEX idx_execution_date (execution_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Agent 执行记录';
