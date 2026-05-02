-- 修复新表的字符集排序规则，与现有表保持一致
ALTER TABLE tu_title_review CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE tu_server_config CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE tu_title_push_log CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
