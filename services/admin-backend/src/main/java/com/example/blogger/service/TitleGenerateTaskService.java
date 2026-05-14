package com.example.blogger.service;

import com.example.blogger.entity.TitleGenerateTask;
import com.example.blogger.mapper.TitleGenerateTaskMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TitleGenerateTaskService {

    private final TitleGenerateTaskMapper taskMapper;

    public TitleGenerateTaskService(TitleGenerateTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public TitleGenerateTask findById(String id) {
        return taskMapper.findById(id);
    }

    public TitleGenerateTask findOnePending() {
        return taskMapper.findOnePending();
    }

    public List<TitleGenerateTask> listTasks(String status) {
        return taskMapper.findAllWithSearch(status);
    }

    public TitleGenerateTask createTask(String platforms, String trackIds, Integer countPerCombo, String instruction) {
        TitleGenerateTask task = new TitleGenerateTask();
        task.setId(UUID.randomUUID().toString().replace("-", ""));
        task.setStatus("pending");
        task.setPlatforms(platforms);
        task.setTrackIds(trackIds);
        task.setCountPerCombo(countPerCombo != null ? countPerCombo : 3);
        task.setInstruction(instruction);
        task.setProgressStep(0);
        task.setProgressMessage("排队中");
        task.setDuplicateCount(0);
        task.setInsertedCount(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        return task;
    }

    public void updateStatus(String id, String status) {
        taskMapper.updateStatus(id, status, LocalDateTime.now());
    }

    public void updateFailed(String id, String errorMessage) {
        taskMapper.updateFailed(id, errorMessage, LocalDateTime.now());
    }

    public void updateProgress(String id, Integer step, String message) {
        taskMapper.updateProgress(id, step, message, LocalDateTime.now());
    }

    public void updateCompleted(String id, String resultFileUrl, String resultFileName, Integer duplicateCount, Integer insertedCount) {
        taskMapper.updateCompleted(id, "completed", resultFileUrl, resultFileName, LocalDateTime.now(), LocalDateTime.now(), duplicateCount, insertedCount);
    }

    public boolean cancelTask(String id) {
        int rows = taskMapper.deletePendingById(id);
        return rows > 0;
    }

    public boolean stopTask(String id) {
        TitleGenerateTask task = taskMapper.findById(id);
        if (task == null || !"processing".equals(task.getStatus())) {
            return false;
        }
        taskMapper.updateStatus(id, "stopped", LocalDateTime.now());
        return true;
    }
}
