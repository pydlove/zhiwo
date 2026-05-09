CREATE TABLE IF NOT EXISTS tu_scheduled_push (
  id VARCHAR(64) PRIMARY KEY,
  push_time VARCHAR(5) NOT NULL,
  status TINYINT DEFAULT 0 COMMENT '0=待执行, 1=执行中, 2=已执行, 3=已取消',
  last_executed_date VARCHAR(10) COMMENT 'yyyy-MM-dd 上次执行日期',
  user_filter_type VARCHAR(20) DEFAULT 'all' COMMENT 'all=全部, selected=指定用户',
  user_ids TEXT COMMENT 'JSON数组，指定用户ID列表',
  created_by VARCHAR(64) COMMENT '创建人admin ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;