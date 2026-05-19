package com.example.blogger.scheduler;

import com.example.blogger.entity.AgentConfig;
import com.example.blogger.entity.AgentExecution;
import com.example.blogger.mapper.AgentConfigMapper;
import com.example.blogger.mapper.AgentExecutionMapper;
import com.example.blogger.service.AgentOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Component
public class AIAgentScheduler {

    private static final Logger log = LoggerFactory.getLogger(AIAgentScheduler.class);

    private final AgentConfigMapper agentConfigMapper;
    private final AgentExecutionMapper agentExecutionMapper;
    private final AgentOrchestrator agentOrchestrator;

    // 记录今天是否已经执行过，避免重复执行
    private volatile LocalDate lastExecutionDate = null;

    @Autowired
    public AIAgentScheduler(AgentConfigMapper agentConfigMapper,
                            AgentExecutionMapper agentExecutionMapper,
                            AgentOrchestrator agentOrchestrator) {
        this.agentConfigMapper = agentConfigMapper;
        this.agentExecutionMapper = agentExecutionMapper;
        this.agentOrchestrator = agentOrchestrator;
    }

    /**
     * 每分钟检查一次是否需要执行 Agent
     */
    @Scheduled(fixedDelay = 60000)
    public void checkAndRun() {
        AgentConfig config = agentConfigMapper.findOne();
        if (config == null || !Integer.valueOf(1).equals(config.getEnabled())) {
            return;
        }

        LocalDate today = LocalDate.now();

        // 今天已经执行过则跳过
        if (today.equals(lastExecutionDate)) {
            return;
        }

        // 检查今天是否已经有执行记录（可能是手动触发的）
        List<AgentExecution> todayExecutions = agentExecutionMapper.findByDate(today.toString());
        boolean alreadyRanToday = todayExecutions.stream()
                .anyMatch(e -> "completed".equals(e.getStatus()) || "running".equals(e.getStatus()));
        if (alreadyRanToday) {
            lastExecutionDate = today;
            return;
        }

        // 解析 cron 表达式，判断当前时间是否匹配
        String cronExpr = config.getCronExpr();
        if (cronExpr == null || cronExpr.isEmpty()) {
            cronExpr = "0 0 6 * * ?";
        }

        if (!isCronMatch(cronExpr)) {
            return;
        }

        log.info("[AIAgentScheduler] 定时触发 Agent 执行, cron={}", cronExpr);
        lastExecutionDate = today;

        try {
            agentOrchestrator.executeAgentRun();
        } catch (Exception e) {
            log.error("[AIAgentScheduler] Agent 执行异常", e);
        }
    }

    /**
     * 手动触发执行（用于测试）
     */
    public void triggerManualRun() {
        log.info("[AIAgentScheduler] 手动触发 Agent 执行");
        lastExecutionDate = LocalDate.now();
        agentOrchestrator.executeAgentRun();
    }

    /**
     * 简单的 cron 匹配：只检查当前分钟是否匹配
     * 支持格式：秒 分 时 日 月 周
     */
    private boolean isCronMatch(String cronExpr) {
        try {
            String[] parts = cronExpr.trim().split("\\s+");
            if (parts.length < 6) return false;

            LocalDateTime now = LocalDateTime.now();

            // 秒
            if (!matchCronField(parts[0], now.getSecond(), 0, 59)) return false;
            // 分
            if (!matchCronField(parts[1], now.getMinute(), 0, 59)) return false;
            // 时
            if (!matchCronField(parts[2], now.getHour(), 0, 23)) return false;
            // 日
            if (!matchCronField(parts[3], now.getDayOfMonth(), 1, 31)) return false;
            // 月
            if (!matchCronField(parts[4], now.getMonthValue(), 1, 12)) return false;
            // 周
            int dayOfWeek = now.getDayOfWeek().getValue(); // 1=周一, 7=周日
            if (!matchCronField(parts[5], dayOfWeek % 7, 0, 6)) return false; // cron 周日=0

            return true;
        } catch (Exception e) {
            log.warn("[AIAgentScheduler] cron 解析失败: {}", cronExpr, e);
            return false;
        }
    }

    private boolean matchCronField(String field, int value, int min, int max) {
        if ("*".equals(field) || "?".equals(field)) return true;

        // 处理列表，如 "1,2,3" 或 "1-5"
        String[] segments = field.split(",");
        for (String seg : segments) {
            if (seg.contains("/")) {
                // 步长，如 */5
                String[] stepParts = seg.split("/");
                int step = Integer.parseInt(stepParts[1]);
                int start = stepParts[0].equals("*") ? min : Integer.parseInt(stepParts[0]);
                if (value >= start && (value - start) % step == 0) return true;
            } else if (seg.contains("-")) {
                // 范围，如 1-5
                String[] range = seg.split("-");
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);
                if (value >= start && value <= end) return true;
            } else {
                // 单个值
                if (Integer.parseInt(seg) == value) return true;
            }
        }
        return false;
    }
}
