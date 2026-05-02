package com.example.blogger.scheduler;

import com.example.blogger.entity.ProcessAutoConfig;
import com.example.blogger.entity.ProcessDailyLog;
import com.example.blogger.entity.TitleLibrary;
import com.example.blogger.entity.Track;
import com.example.blogger.mapper.TitleLibraryMapper;
import com.example.blogger.mapper.TrackMapper;
import com.example.blogger.service.ProcessAutoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ProcessAutoScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProcessAutoScheduler.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ProcessAutoService processAutoService;
    private final TitleLibraryMapper titleLibraryMapper;
    private final TrackMapper trackMapper;

    // 记录今天是否已经触发过，避免同一分钟内重复触发
    private LocalDate lastTriggeredDate = null;

    public ProcessAutoScheduler(ProcessAutoService processAutoService,
                                TitleLibraryMapper titleLibraryMapper,
                                TrackMapper trackMapper) {
        this.processAutoService = processAutoService;
        this.titleLibraryMapper = titleLibraryMapper;
        this.trackMapper = trackMapper;
    }

    /**
     * 每分钟检查一次是否到达配置的触发时间
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkTriggerTime() {
        ProcessAutoConfig config = processAutoService.getConfig();
        if (config == null || !Integer.valueOf(1).equals(config.getIsEnabled())) {
            return;
        }

        String checkTime = config.getCheckTime();
        if (checkTime == null || checkTime.isEmpty()) {
            checkTime = "03:00";
        }

        String nowTime = LocalDateTime.now().format(TIME_FORMATTER);
        LocalDate today = LocalDate.now();

        // 时间匹配且今天未触发过
        if (checkTime.equals(nowTime) && !today.equals(lastTriggeredDate)) {
            lastTriggeredDate = today;
            log.info("[ProcessAutoScheduler] 到达配置时间 {}，开始执行标题检查", checkTime);
            doCheck(config);
        }
    }

    /**
     * 执行标题检查逻辑
     */
    public void doCheck(ProcessAutoConfig config) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        ProcessDailyLog dailyLog = processAutoService.getOrCreateLog(tomorrow);

        // 如果今天已经检查过了，跳过
        if (dailyLog.getCheckTime() != null && dailyLog.getCheckTime().toLocalDate().equals(LocalDate.now())) {
            log.info("[ProcessAutoScheduler] 今天已经检查过了，跳过");
            return;
        }

        dailyLog.setCheckTime(LocalDateTime.now());
        dailyLog.setStatus("checking");

        // 获取明天推荐日期的标题
        String pushDateStr = tomorrow.toString();
        List<TitleLibrary> tomorrowTitles = titleLibraryMapper.findByPushDate(pushDateStr);

        // 获取需要检查的赛道
        List<Track> tracks;
        if (Integer.valueOf(1).equals(config.getCheckAllTracks())) {
            tracks = trackMapper.findAll();
        } else {
            // 只检查已订阅的赛道（简化处理，先查全部）
            tracks = trackMapper.findAll();
        }

        int titlesPerTrack = config.getTitlesPerTrack() != null ? config.getTitlesPerTrack() : 3;
        int expectedTotal = tracks.size() * titlesPerTrack;
        int actualTotal = tomorrowTitles != null ? tomorrowTitles.size() : 0;

        log.info("[ProcessAutoScheduler] 目标日期: {}, 赛道数: {}, 预期标题数: {}, 实际标题数: {}",
                tomorrow, tracks.size(), expectedTotal, actualTotal);

        if (actualTotal < expectedTotal) {
            dailyLog.setStatus("need_titles");
            dailyLog.setTitlesNeeded(expectedTotal - actualTotal);
            log.info("[ProcessAutoScheduler] 标题不足，需要生成 {} 条", expectedTotal - actualTotal);
        } else {
            dailyLog.setStatus("matching");
            log.info("[ProcessAutoScheduler] 标题充足，进入匹配阶段");
        }

        processAutoService.updateLog(dailyLog);
    }
}
