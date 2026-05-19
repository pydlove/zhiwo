package com.example.blogger.controller;

import com.example.blogger.entity.AgentConfig;
import com.example.blogger.mapper.AgentConfigMapper;
import com.example.blogger.entity.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentConfigController {

    private static final Logger log = LoggerFactory.getLogger(AgentConfigController.class);
    private final AgentConfigMapper agentConfigMapper;

    @Autowired
    public AgentConfigController(AgentConfigMapper agentConfigMapper) {
        this.agentConfigMapper = agentConfigMapper;
    }

    @GetMapping("/config")
    public Result<AgentConfig> getConfig() {
        try {
            AgentConfig config = agentConfigMapper.findOne();
            if (config == null) {
                return Result.error("配置不存在");
            }
            return Result.ok(config);
        } catch (Exception e) {
            log.error("[AgentConfigController] 获取配置失败", e);
            return Result.error("获取配置失败: " + e.getMessage());
        }
    }

    @PostMapping("/config")
    public Result<Void> saveConfig(@RequestBody AgentConfig config) {
        try {
            // 确保 ID 为 1
            config.setId(1L);
            agentConfigMapper.save(config);
            log.info("[AgentConfigController] 配置保存成功: enabled={}", config.getEnabled());
            return Result.ok(null);
        } catch (Exception e) {
            log.error("[AgentConfigController] 保存配置失败", e);
            return Result.error("保存配置失败: " + e.getMessage());
        }
    }
}
