package com.example.blogger.service;

import com.example.blogger.entity.TitleGenerationTask;
import com.example.blogger.mapper.TitleGenerationTaskMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TitleGenerationTaskService {

    private final TitleGenerationTaskMapper taskMapper;
    private final TaskInterruptManager interruptManager;

    public TitleGenerationTaskService(TitleGenerationTaskMapper taskMapper, TaskInterruptManager interruptManager) {
        this.taskMapper = taskMapper;
        this.interruptManager = interruptManager;
    }

    public TitleGenerationTask findById(String id) {
        return taskMapper.findById(id);
    }

    public List<TitleGenerationTask> findByTitleLibraryId(String titleLibraryId) {
        return taskMapper.findByTitleLibraryId(titleLibraryId);
    }

    public TitleGenerationTask findOnePending() {
        return taskMapper.findOnePending();
    }

    public List<TitleGenerationTask> findAll() {
        return taskMapper.findAll();
    }

    public TitleGenerationTask createTask(String titleLibraryId, String title, String prompt) {
        TitleGenerationTask task = new TitleGenerationTask();
        task.setId(UUID.randomUUID().toString().replace("-", ""));
        task.setTitleLibraryId(titleLibraryId);
        task.setTitle(title);
        task.setPrompt(prompt);
        task.setStatus("pending");
        task.setProgressStep(0);
        task.setProgressMessage("排队中");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        return task;
    }

    public void updateStatus(String id, String status, String resultFileUrl) {
        if ("completed".equals(status)) {
            String fileName = resultFileUrl != null ? resultFileUrl.substring(resultFileUrl.lastIndexOf("/") + 1) : null;
            taskMapper.updateCompleted(id, status, resultFileUrl, fileName, LocalDateTime.now(), LocalDateTime.now());
        } else {
            taskMapper.updateStatus(id, status, LocalDateTime.now());
        }
    }

    public void updateFailed(String id, String errorMessage) {
        taskMapper.updateFailed(id, errorMessage, LocalDateTime.now());
    }

    public void updateProgress(String id, Integer step, String message) {
        taskMapper.updateProgress(id, step, message, LocalDateTime.now());
    }

    public void updateGeneratedContent(String id, String content) {
        taskMapper.updateGeneratedContent(id, content, LocalDateTime.now());
    }

    public List<TitleGenerationTask> listTasks(String keyword, String status) {
        return taskMapper.findAllWithSearch(keyword, status);
    }

    public boolean cancelTask(String id) {
        int rows = taskMapper.deletePendingById(id);
        return rows > 0;
    }

    public boolean stopTask(String id) {
        TitleGenerationTask task = taskMapper.findById(id);
        if (task == null || !"processing".equals(task.getStatus())) {
            return false;
        }
        taskMapper.updateStatus(id, "stopped", LocalDateTime.now());
        // 中断正在执行该任务的线程（如果任务正处于 LLM 调用阻塞中）
        interruptManager.interrupt(id);
        return true;
    }

    public TitleGenerationTask retryTask(String id) {
        TitleGenerationTask task = taskMapper.findById(id);
        if (task == null) {
            return null;
        }
        TitleGenerationTask newTask = new TitleGenerationTask();
        newTask.setId(UUID.randomUUID().toString().replace("-", ""));
        newTask.setTitleLibraryId(task.getTitleLibraryId());
        newTask.setTitle(task.getTitle());
        newTask.setPrompt(task.getPrompt());
        newTask.setStatus("pending");
        newTask.setProgressStep(0);
        newTask.setProgressMessage("排队中");
        newTask.setCreatedAt(LocalDateTime.now());
        newTask.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(newTask);
        return newTask;
    }
}
