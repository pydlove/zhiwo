package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.ServerConfig;
import com.example.blogger.mapper.ServerConfigMapper;
import com.example.blogger.service.TitleReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/server-configs")
@CrossOrigin(origins = "*")
public class ServerConfigController {

    private final ServerConfigMapper serverConfigMapper;
    private final TitleReviewService titleReviewService;

    public ServerConfigController(ServerConfigMapper serverConfigMapper, TitleReviewService titleReviewService) {
        this.serverConfigMapper = serverConfigMapper;
        this.titleReviewService = titleReviewService;
    }

    @GetMapping
    public Result<List<ServerConfig>> list() {
        return Result.ok(serverConfigMapper.findAll());
    }

    @PostMapping
    public Result<Void> save(@RequestBody ServerConfig config) {
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId(UUID.randomUUID().toString().replace("-", ""));
            config.setIsActive(config.getIsActive() != null ? config.getIsActive() : 1);
            config.setIsDefault(config.getIsDefault() != null ? config.getIsDefault() : 0);
            serverConfigMapper.insert(config);
        } else {
            serverConfigMapper.update(config);
        }
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        serverConfigMapper.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/{id}/test")
    public Result<Map<String, Object>> testConnection(@PathVariable String id) {
        ServerConfig config = serverConfigMapper.findById(id);
        if (config == null) {
            return Result.error("配置不存在");
        }
        boolean success = titleReviewService.testConnection(config);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "连接成功" : "连接失败，请检查配置");
        return Result.ok(result);
    }

    @PostMapping("/test-direct")
    public Result<Map<String, Object>> testDirectConnection(@RequestBody ServerConfig config) {
        boolean success = titleReviewService.testConnection(config);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "连接成功" : "连接失败，请检查配置");
        return Result.ok(result);
    }
}
