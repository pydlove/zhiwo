package com.example.blogger.service;

import com.example.blogger.entity.ProcessAutoConfig;
import com.example.blogger.entity.ProcessDailyLog;
import com.example.blogger.mapper.ProcessAutoConfigMapper;
import com.example.blogger.mapper.ProcessDailyLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ProcessAutoService {

    private final ProcessAutoConfigMapper configMapper;
    private final ProcessDailyLogMapper dailyLogMapper;

    public ProcessAutoService(ProcessAutoConfigMapper configMapper, ProcessDailyLogMapper dailyLogMapper) {
        this.configMapper = configMapper;
        this.dailyLogMapper = dailyLogMapper;
    }

    public ProcessAutoConfig getConfig() {
        return configMapper.findOne();
    }

    public void saveConfig(ProcessAutoConfig config) {
        configMapper.update(config);
    }

    public ProcessDailyLog getOrCreateLog(LocalDate targetDate) {
        if (targetDate == null) {
            targetDate = LocalDate.now().plusDays(1);
        }
        ProcessDailyLog log = dailyLogMapper.findByTargetDate(targetDate);
        if (log == null) {
            log = new ProcessDailyLog();
            log.setId(UUID.randomUUID().toString().replace("-", ""));
            log.setTargetDate(targetDate);
            log.setStatus("waiting");
            log.setTitlesNeeded(0);
            log.setTitlesGenerated(0);
            log.setTitlesApproved(0);
            log.setTitlesPushed(0);
            log.setTitlesMatched(0);
            log.setArticlesNeeded(0);
            log.setArticlesUploaded(0);
            log.setPushSuccess(0);
            log.setPushFailed(0);
            dailyLogMapper.insert(log);
        }
        return log;
    }

    public ProcessDailyLog getTodayLog() {
        return getOrCreateLog(LocalDate.now().plusDays(1));
    }

    public List<ProcessDailyLog> getRecentLogs(int limit) {
        return dailyLogMapper.findRecent(limit);
    }

    public void updateLog(ProcessDailyLog log) {
        dailyLogMapper.update(log);
    }

    /**
     * 触发一次标题检查
     */
    public ProcessDailyLog triggerCheck(LocalDate targetDate) {
        ProcessDailyLog log = getOrCreateLog(targetDate);
        log.setCheckTime(java.time.LocalDateTime.now());
        log.setStatus("checking");
        dailyLogMapper.update(log);
        return log;
    }

    /**
     * 更新日志状态
     */
    public void updateLogStatus(String logId, String status) {
        ProcessDailyLog log = new ProcessDailyLog();
        log.setId(logId);
        log.setStatus(status);
        dailyLogMapper.update(log);
    }
}
