package com.example.blogger.controller;

import com.example.blogger.entity.Config;
import com.example.blogger.entity.Result;
import com.example.blogger.mapper.ConfigMapper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configs")
@CrossOrigin(origins = "*")
public class ConfigController {
    private final ConfigMapper configMapper;

    public ConfigController(ConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    @GetMapping
    public Result<Map<String, String>> list() {
        List<Config> list = configMapper.findAll();
        Map<String, String> map = new HashMap<>();
        for (Config c : list) {
            map.put(c.getConfigKey(), c.getConfigValue());
        }
        return Result.ok(map);
    }

    @PostMapping
    public Result<Void> save(@RequestBody Map<String, String> req) {
        for (Map.Entry<String, String> entry : req.entrySet()) {
            Config c = new Config();
            c.setConfigKey(entry.getKey());
            c.setConfigValue(entry.getValue());
            configMapper.save(c);
        }
        return Result.ok(null);
    }
}
