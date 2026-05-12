-- 为标题生成任务表添加进度跟踪字段
ALTER TABLE tu_title_generation_task
    ADD COLUMN progress_step INT DEFAULT 0 COMMENT '进度步骤：0=排队 1=构建提示词 2=大模型生成 3=写入文件 4=完成',
    ADD COLUMN progress_message VARCHAR(200) DEFAULT '' COMMENT '当前进度描述';
