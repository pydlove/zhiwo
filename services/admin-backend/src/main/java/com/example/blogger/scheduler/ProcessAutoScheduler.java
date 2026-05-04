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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "process.auto.scheduler.enabled", havingValue = "true", matchIfMissing = true)
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
     * 执行标题检查逻辑（按平台+赛道组合检查）
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

        // 按 platform + trackId 分组统计实际标题数
        Map<String, Long> actualCountMap = new HashMap<>();
        if (tomorrowTitles != null) {
            for (TitleLibrary t : tomorrowTitles) {
                String platform = t.getPlatform() != null ? t.getPlatform() : "";
                String key = platform + ":" + t.getTrackId();
                actualCountMap.put(key, actualCountMap.getOrDefault(key, 0L) + 1);
            }
        }

        // 获取各组合下已经有关联文章的标题数量（通过 recommend_date = tomorrow 且 subscription_post_id IS NOT NULL）
        Map<String, Long> completedCountMap = new HashMap<>();
        List<Map<String, Object>> completedList = titleLibraryMapper.countCompletedByCombo(pushDateStr);
        if (completedList != null) {
            for (Map<String, Object> row : completedList) {
                String platform = row.get("platform") != null ? row.get("platform").toString() : "";
                String trackId = row.get("trackId") != null ? row.get("trackId").toString() : "";
                Long cnt = ((Number) row.get("cnt")).longValue();
                String key = platform + ":" + trackId;
                completedCountMap.put(key, cnt);
            }
        }

        // 获取配置要检查的平台列表
        List<String> configPlatforms = new ArrayList<>();
        if (config.getCheckPlatforms() != null && !config.getCheckPlatforms().trim().isEmpty()) {
            configPlatforms = Arrays.asList(config.getCheckPlatforms().trim().split(","));
        }

        // 获取需要检查的赛道
        List<Track> tracks;
        if (Integer.valueOf(1).equals(config.getCheckAllTracks())) {
            tracks = trackMapper.findAll();
        } else {
            // 只检查已订阅的赛道（简化处理，先查全部）
            tracks = trackMapper.findAll();
        }

        int titlesPerTrack = config.getTitlesPerTrack() != null ? config.getTitlesPerTrack() : 3;

        int expectedTotal = 0;
        int actualTotal = 0;
        int missingTotal = 0;
        List<Map<String, Object>> missingList = new ArrayList<>();

        for (Track track : tracks) {
            // 解析赛道支持的平台
            List<String> trackPlatforms = new ArrayList<>();
            if (track.getPlatforms() != null && !track.getPlatforms().trim().isEmpty()) {
                trackPlatforms = Arrays.asList(track.getPlatforms().trim().split(","));
            }

            // 取交集：配置要检查的平台 ∩ 赛道支持的平台
            List<String> platformsToCheck;
            if (configPlatforms.isEmpty()) {
                platformsToCheck = trackPlatforms;
            } else {
                platformsToCheck = trackPlatforms.stream()
                        .filter(configPlatforms::contains)
                        .collect(Collectors.toList());
            }

            for (String platform : platformsToCheck) {
                expectedTotal += titlesPerTrack;
                String key = platform + ":" + track.getId();
                long total = actualCountMap.getOrDefault(key, 0L);
                long completed = completedCountMap.getOrDefault(key, 0L);
                long available = Math.max(total - completed, 0L);
                actualTotal += available;
                if (available < titlesPerTrack) {
                    int missing = titlesPerTrack - (int) available;
                    missingTotal += missing;
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("platform", platform);
                    detail.put("trackId", track.getId());
                    detail.put("trackName", track.getName());
                    detail.put("needed", titlesPerTrack);
                    detail.put("total", (int) total);
                    detail.put("completed", (int) completed);
                    detail.put("available", (int) available);
                    detail.put("missing", missing);
                    missingList.add(detail);
                }
            }
        }

        int comboCount = expectedTotal / Math.max(titlesPerTrack, 1);
        log.info("[ProcessAutoScheduler] 目标日期: {}, 平台+赛道组合: {}, 预期: {}, 可用: {}, 缺少: {}",
                tomorrow, comboCount, expectedTotal, actualTotal, missingTotal);

        if (missingTotal > 0) {
            dailyLog.setStatus("need_titles");
            dailyLog.setTitlesNeeded(missingTotal);
            // 把缺少详情序列化为简单 JSON 存入 errorMsg
            dailyLog.setErrorMsg(serializeMissingList(missingList));
            log.info("[ProcessAutoScheduler] 标题不足，需要生成 {} 条，涉及 {} 个组合", missingTotal, missingList.size());
            for (Map<String, Object> m : missingList) {
                log.info("[ProcessAutoScheduler]   - {}+{}: 缺{}条 (可用{}=总{}-已完成{} / 需要{})",
                        m.get("platform"), m.get("trackName"), m.get("missing"),
                        m.get("available"), m.get("total"), m.get("completed"), m.get("needed"));
            }
        } else {
            dailyLog.setStatus("matching");
            dailyLog.setTitlesNeeded(0);
            dailyLog.setErrorMsg(null);
            log.info("[ProcessAutoScheduler] 标题充足，进入匹配阶段");
        }

        processAutoService.updateLog(dailyLog);
    }

    private String serializeMissingList(List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> m = list.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"platform\":\"").append(escapeJson(m.get("platform").toString())).append("\",");
            sb.append("\"trackId\":\"").append(escapeJson(m.get("trackId").toString())).append("\",");
            sb.append("\"trackName\":\"").append(escapeJson(m.get("trackName").toString())).append("\",");
            sb.append("\"needed\":").append(m.get("needed")).append(",");
            sb.append("\"total\":").append(m.get("total")).append(",");
            sb.append("\"completed\":").append(m.get("completed")).append(",");
            sb.append("\"available\":").append(m.get("available")).append(",");
            sb.append("\"missing\":").append(m.get("missing"));
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
