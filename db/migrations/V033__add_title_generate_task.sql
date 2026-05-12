-- V2 标题生成任务表
CREATE TABLE IF NOT EXISTS tu_title_generate_task (
    id VARCHAR(32) PRIMARY KEY COMMENT '任务ID',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '任务状态：pending/processing/completed/failed/stopped',
    platforms VARCHAR(500) DEFAULT NULL COMMENT '平台列表，JSON数组字符串',
    track_ids VARCHAR(500) DEFAULT NULL COMMENT '赛道ID列表，JSON数组字符串',
    count_per_combo INT DEFAULT 3 COMMENT '每个组合生成数量',
    instruction VARCHAR(1000) DEFAULT NULL COMMENT '生成方向/提示',
    result_file_url VARCHAR(255) DEFAULT NULL COMMENT '生成结果Excel文件URL',
    result_file_name VARCHAR(255) DEFAULT NULL COMMENT '生成结果Excel文件名',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    progress_step INT DEFAULT 0 COMMENT '进度步骤：0=排队 1=准备数据 2=大模型生成 3=解析入库 4=生成Excel 5=完成',
    progress_message VARCHAR(200) DEFAULT '' COMMENT '当前进度描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    processed_at DATETIME DEFAULT NULL COMMENT '完成时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V2标题生成任务表';

CREATE INDEX idx_tg_task_status ON tu_title_generate_task(status);
CREATE INDEX idx_tg_task_created_at ON tu_title_generate_task(created_at);
