package com.example.blogger.controller;

import com.example.blogger.entity.AgentExecution;
import com.example.blogger.mapper.AgentExecutionMapper;
import com.example.blogger.scheduler.AIAgentScheduler;
import com.example.blogger.entity.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentExecutionController {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutionController.class);
    private final AgentExecutionMapper agentExecutionMapper;
    private final AIAgentScheduler agentScheduler;

    @Autowired
    public AgentExecutionController(AgentExecutionMapper agentExecutionMapper, AIAgentScheduler agentScheduler) {
        this.agentExecutionMapper = agentExecutionMapper;
        this.agentScheduler = agentScheduler;
    }

    @GetMapping("/executions")
    public Result<List<AgentExecution>> getExecutions(@RequestParam(defaultValue = "20") int limit) {
        try {
            List<AgentExecution> list = agentExecutionMapper.findRecent(limit);
            return Result.ok(list);
        } catch (Exception e) {
            log.error("[AgentExecutionController] 查询执行记录失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/executions/{id}")
    public Result<AgentExecution> getExecution(@PathVariable Long id) {
        try {
            AgentExecution execution = agentExecutionMapper.findById(id);
            if (execution == null) {
                return Result.error("记录不存在");
            }
            return Result.ok(execution);
        } catch (Exception e) {
            log.error("[AgentExecutionController] 查询执行记录详情失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/trigger")
    public Result<Map<String, Object>> triggerRun() {
        try {
            log.info("[AgentExecutionController] 手动触发 Agent 执行");
            // 异步执行，避免阻塞接口
            new Thread(() -> agentScheduler.triggerManualRun()).start();

            Map<String, Object> result = new HashMap<>();
            result.put("message", "Agent 执行已触发，请稍后查看执行记录");
            result.put("date", LocalDate.now().toString());
            return Result.ok(result);
        } catch (Exception e) {
            log.error("[AgentExecutionController] 手动触发失败", e);
            return Result.error("触发失败: " + e.getMessage());
        }
    }
}
