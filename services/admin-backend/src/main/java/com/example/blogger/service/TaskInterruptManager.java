package com.example.blogger.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理正在执行生成任务的线程，用于支持"停止"操作时中断 LLM 调用。
 */
@Component
public class TaskInterruptManager {

    private final ConcurrentHashMap<String, Thread> runningThreads = new ConcurrentHashMap<>();

    public void register(String taskId, Thread thread) {
        runningThreads.put(taskId, thread);
    }

    public void unregister(String taskId) {
        runningThreads.remove(taskId);
    }

    public void interrupt(String taskId) {
        Thread thread = runningThreads.get(taskId);
        if (thread != null) {
            thread.interrupt();
        }
    }
}
