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

@Service
public class ScheduledPushService {
    private final ScheduledPushMapper scheduledPushMapper;
    private final UserMapper userMapper;
    private final TitleLibraryService titleLibraryService;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

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
        if (tasks.isEmpty()) return;

        LocalTime now = LocalTime.now();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        for (ScheduledPush task : tasks) {
            LocalTime pushTime = LocalTime.parse(task.getPushTime(), TIME_FORMATTER);
            if (now.isBefore(pushTime)) continue; // 未到执行时间
            if (today.equals(task.getLastExecutedDate())) continue; // 今日已执行

            executeTask(task, today);
        }
    }

    private void executeTask(ScheduledPush task, String today) {
        try {
            scheduledPushMapper.updateStatus(task.getId(), 1, null); // 1=执行中
            List<String> userIds = resolveUserIds(task);
            if (userIds.isEmpty()) {
                scheduledPushMapper.updateStatus(task.getId(), 0, today); // 仍为待执行，但标记今日已执行
                return;
            }
            // 推送日期为今天
            titleLibraryService.batchPushEmailForScheduled(today, userIds);
            scheduledPushMapper.updateStatus(task.getId(), 0, today); // 执行完重置为待执行，标记今日已执行
        } catch (Exception e) {
            // 执行失败，重置为待执行，下次再试
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
