package com.example.user.controller;

import com.example.user.entity.Config;
import com.example.user.entity.Result;
import com.example.user.mapper.ConfigMapper;
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
}
