package com.example.user.controller;

import com.example.user.entity.Config;
import com.example.user.entity.Result;
import com.example.user.entity.Track;
import com.example.user.mapper.ConfigMapper;
import com.example.user.service.TrackService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/open-account")
@CrossOrigin(origins = "*")
public class OpenAccountController {

    private final ConfigMapper configMapper;
    private final TrackService trackService;

    public OpenAccountController(ConfigMapper configMapper, TrackService trackService) {
        this.configMapper = configMapper;
        this.trackService = trackService;
    }

    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig(@RequestParam("code") String code) {
        if (code == null || code.trim().isEmpty()) {
            return Result.error("链接码不能为空");
        }
        Config config = configMapper.findByKey("oa_link_" + code.trim());
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isEmpty()) {
            return Result.error("链接已失效或不存在");
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> data = mapper.readValue(config.getConfigValue(), Map.class);
            return Result.ok(data);
        } catch (Exception e) {
            return Result.error("链接数据解析失败");
        }
    }

    @GetMapping("/short-links/{code}")
    public Result<Map<String, String>> resolveShortLink(@PathVariable String code) {
        if (code == null || code.trim().isEmpty()) {
            return Result.error("短链码不能为空");
        }
        Config config = configMapper.findByKey("op_link_" + code.trim());
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isEmpty()) {
            return Result.error("短链已失效或不存在");
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> data = mapper.readValue(config.getConfigValue(), Map.class);
            String baseUrl = data.get("baseUrl") != null ? data.get("baseUrl").toString() : "http://www.mmshuo.tech";
            String targetPath = data.get("targetPath") != null ? data.get("targetPath").toString() : "/login";
            String username = data.get("username") != null ? data.get("username").toString() : "";
            String targetUrl = baseUrl + targetPath + "?op=" + java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8);
            Map<String, String> result = new HashMap<>();
            result.put("targetUrl", targetUrl);
            result.put("username", username);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.error("短链数据解析失败");
        }
    }

    @GetMapping("/tracks")
    public Result<List<Track>> listTracks(@RequestParam(value = "platform", required = false) String platform) {
        List<Track> all = trackService.list();
        if (platform == null || platform.isEmpty()) {
            return Result.ok(all);
        }
        List<Track> filtered = new ArrayList<>();
        for (Track t : all) {
            String platforms = t.getPlatforms();
            if (platforms != null && platforms.contains(platform)) {
                filtered.add(t);
            }
        }
        return Result.ok(filtered);
    }
}
