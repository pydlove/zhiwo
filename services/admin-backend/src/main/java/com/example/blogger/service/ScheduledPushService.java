package com.example.blogger.service;

import com.example.blogger.entity.ScheduledPush;
import com.example.blogger.mapper.ScheduledPushMapper;
import com.example.blogger.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ScheduledPushService {
    private static final Logger log = LoggerFactory.getLogger(ScheduledPushService.class);

    private final ScheduledPushMapper scheduledPushMapper;
    private final UserMapper userMapper;
    private final TitleLibraryService titleLibraryService;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_FORMATTER_WITH_SEC = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ScheduledPushService(ScheduledPushMapper scheduledPushMapper, UserMapper userMapper,
                                TitleLibraryService titleLibraryService, ObjectMapper objectMapper) {
        this.scheduledPushMapper = scheduledPushMapper;
        this.userMapper = userMapper;
        this.titleLibraryService = titleLibraryService;
        this.objectMapper = objectMapper;
    }

    public void create(String pushTime, String userFilterType, List<String> userIds, String createdBy) {
        ScheduledPush push = new ScheduledPush();
        push.setId(UUID.randomUUID().toString().replace("-", ""));
        push.setPushTime(pushTime);
        push.setStatus(0);
        push.setUserFilterType(userFilterType != null ? userFilterType : "all");
        try {
            push.setUserIds(userIds != null ? objectMapper.writeValueAsString(userIds) : null);
        } catch (Exception e) {
            push.setUserIds("[]");
        }
        push.setCreatedBy(createdBy);
        scheduledPushMapper.insert(push);
    }

    public void cancel(String id) {
        scheduledPushMapper.updateStatus(id, 3, null);
    }

    public void updatePushTime(String id, String pushTime) {
        ScheduledPush push = scheduledPushMapper.findById(id);
        if (push == null) {
            throw new RuntimeException("任务不存在");
        }
        if (push.getStatus() != null && push.getStatus() != 0) {
            throw new RuntimeException("只能编辑待执行状态的任务");
        }
        scheduledPushMapper.updatePushTime(id, pushTime);
    }

    public void triggerNow(String id) {
        ScheduledPush task = scheduledPushMapper.findById(id);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log.info("[ScheduledPush] 手动触发任务: id={}, pushTime={}", id, task.getPushTime());
        executeTask(task, today);
    }

    public void delete(String id) {
        ScheduledPush push = scheduledPushMapper.findById(id);
        if (push == null) {
            throw new RuntimeException("任务不存在");
        }
        if (push.getStatus() != null && push.getStatus() != 3) {
            throw new RuntimeException("只能删除已取消的任务");
        }
        scheduledPushMapper.deleteById(id);
    }

    public Map<String, Object> list(Integer status, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<ScheduledPush> list;
        int total;
        if (status != null) {
            list = scheduledPushMapper.findByStatus(status, pageSize, offset);
            total = scheduledPushMapper.countByStatus(status);
        } else {
            list = scheduledPushMapper.findAll(pageSize, offset);
            total = scheduledPushMapper.countAll();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    public ScheduledPush getById(String id) {
        return scheduledPushMapper.findById(id);
    }

    public void executePending() {
        List<ScheduledPush> tasks = scheduledPushMapper.findActive();
        if (tasks.isEmpty()) {
            log.debug("[ScheduledPush] 无待执行定时推送任务");
            return;
        }

        LocalTime now = LocalTime.now();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log.debug("[ScheduledPush] 开始检查定时推送任务，当前时间={}, 日期={}, 待执行任务数={}", now, today, tasks.size());

        for (ScheduledPush task : tasks) {
            try {
                LocalTime pushTime = parsePushTime(task.getPushTime());
                if (pushTime == null) {
                    log.warn("[ScheduledPush] 任务 pushTime 解析失败，跳过: id={}, pushTime={}", task.getId(), task.getPushTime());
                    continue;
                }
                if (now.isBefore(pushTime)) {
                    log.debug("[ScheduledPush] 任务未到执行时间，跳过: id={}, pushTime={}, now={}", task.getId(), pushTime, now);
                    continue;
                }
                if (today.equals(task.getLastExecutedDate())) {
                    log.debug("[ScheduledPush] 任务今日已执行，跳过: id={}, lastExecutedDate={}", task.getId(), task.getLastExecutedDate());
                    continue;
                }
                log.info("[ScheduledPush] 触发执行任务: id={}, pushTime={}, now={}", task.getId(), pushTime, now);
                executeTask(task, today);
            } catch (Exception e) {
                log.error("[ScheduledPush] 任务处理异常，跳过: id={}", task.getId(), e);
            }
        }
    }

    private LocalTime parsePushTime(String pushTime) {
        if (pushTime == null || pushTime.isEmpty()) return null;
        try {
            if (pushTime.length() == 5) {
                return LocalTime.parse(pushTime, TIME_FORMATTER);
            }
            return LocalTime.parse(pushTime, TIME_FORMATTER_WITH_SEC);
        } catch (Exception e) {
            log.warn("[ScheduledPush] pushTime 解析失败: value={}, error={}", pushTime, e.getMessage());
            return null;
        }
    }

    private void executeTask(ScheduledPush task, String today) {
        try {
            scheduledPushMapper.updateStatus(task.getId(), 1, null); // 1=执行中
            List<String> userIds = resolveUserIds(task);
            log.info("[ScheduledPush] 任务解析用户: id={}, userCount={}", task.getId(), userIds.size());
            if (userIds.isEmpty()) {
                log.warn("[ScheduledPush] 无有效用户，标记今日已执行: id={}", task.getId());
                scheduledPushMapper.updateStatus(task.getId(), 0, today);
                return;
            }
            titleLibraryService.batchPushEmailForScheduled(today, userIds);
            log.info("[ScheduledPush] 任务执行完成，标记今日已执行: id={}, date={}", task.getId(), today);
            scheduledPushMapper.updateStatus(task.getId(), 0, today); // 执行完重置为待执行，标记今日已执行
        } catch (Exception e) {
            log.error("[ScheduledPush] 任务执行失败，重置状态: id={}", task.getId(), e);
            scheduledPushMapper.updateStatus(task.getId(), 0, null);
        }
    }

    private List<String> resolveUserIds(ScheduledPush task) {
        if ("selected".equals(task.getUserFilterType())) {
            if (task.getUserIds() == null || task.getUserIds().isEmpty()) {
                return Collections.emptyList();
            }
            try {
                return objectMapper.readValue(task.getUserIds(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
        // all: 获取所有活跃有效用户
        List<Map<String, Object>> activeUsers = userMapper.findAllActiveUsersWithEmail();
        List<String> result = new ArrayList<>();
        for (Map<String, Object> u : activeUsers) {
            if (u.get("id") != null) result.add(u.get("id").toString());
        }
        return result;
    }
}
