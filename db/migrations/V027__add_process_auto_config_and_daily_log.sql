CREATE TABLE IF NOT EXISTS process_auto_config (
    id VARCHAR(32) PRIMARY KEY,
    check_time VARCHAR(10) DEFAULT '03:00' COMMENT '每日检查时间 HH:mm',
    check_platforms VARCHAR(200) DEFAULT '' COMMENT '检查的平台，逗号分隔，空=全部',
    check_all_tracks TINYINT DEFAULT 1 COMMENT '1=全部赛道，0=只检查已订阅',
    auto_notify_local TINYINT DEFAULT 1 COMMENT '不足时是否通知本地生成',
    titles_per_track INT DEFAULT 3 COMMENT '每个赛道生成数量',
    auto_push_after_approve TINYINT DEFAULT 1 COMMENT '审核后自动推送',
    auto_match_after_push TINYINT DEFAULT 1 COMMENT '推送后自动匹配',
    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='自动化流程配置';

INSERT INTO process_auto_config (id, check_time, check_platforms, check_all_tracks, auto_notify_local, titles_per_track, auto_push_after_approve, auto_match_after_push, is_enabled)
VALUES ('1', '03:00', '', 1, 1, 3, 1, 1, 1)
ON DUPLICATE KEY UPDATE id=id;

CREATE TABLE IF NOT EXISTS process_daily_log (
    id VARCHAR(32) PRIMARY KEY,
    target_date DATE NOT NULL COMMENT '目标推荐日期',
    check_time DATETIME COMMENT '实际触发检查时间',
    status VARCHAR(50) DEFAULT 'checking' COMMENT '当前阶段状态',
    titles_needed INT DEFAULT 0 COMMENT '需要生成的标题数',
    titles_generated INT DEFAULT 0 COMMENT '实际生成的标题数',
    titles_approved INT DEFAULT 0 COMMENT '审核通过数',
    titles_pushed INT DEFAULT 0 COMMENT '已推送数',
    titles_matched INT DEFAULT 0 COMMENT '已匹配用户数',
    articles_needed INT DEFAULT 0 COMMENT '需要生成的文章数',
    articles_uploaded INT DEFAULT 0 COMMENT '已上传文章数',
    push_scheduled_time DATETIME COMMENT '推送排期时间',
    push_success INT DEFAULT 0 COMMENT '推送成功数',
    push_failed INT DEFAULT 0 COMMENT '推送失败数',
    error_msg TEXT COMMENT '错误信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_target_date (target_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日流程执行记录';
