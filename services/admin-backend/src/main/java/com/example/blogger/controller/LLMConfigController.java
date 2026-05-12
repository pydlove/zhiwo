package com.example.blogger.controller;

import com.example.blogger.entity.LLMConfig;
import com.example.blogger.entity.Result;
import com.example.blogger.mapper.LLMConfigMapper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/llm-config")
@CrossOrigin(origins = "*")
public class LLMConfigController {

    private final LLMConfigMapper llmConfigMapper;

    public LLMConfigController(LLMConfigMapper llmConfigMapper) {
        this.llmConfigMapper = llmConfigMapper;
    }

    @GetMapping
    public Result<Map<String, Object>> getConfig() {
        List<LLMConfig> list = llmConfigMapper.findAll();
        Map<String, Object> result = new HashMap<>();
        String selectedProvider = "kimi";

        for (LLMConfig cfg : list) {
            Map<String, String> providerConfig = new HashMap<>();
            providerConfig.put("apiKey", cfg.getApiKey());
            providerConfig.put("model", cfg.getModel());
            result.put(cfg.getProvider(), providerConfig);
            if (cfg.getIsActive() != null && cfg.getIsActive() == 1) {
                selectedProvider = cfg.getProvider();
            }
        }
        result.put("selectedProvider", selectedProvider);
        return Result.ok(result);
    }

    @PostMapping
    public Result<Void> saveConfig(@RequestBody Map<String, Object> req) {
        String selectedProvider = req.get("selectedProvider") != null ? req.get("selectedProvider").toString() : "kimi";

        // Save kimi config
        Object kimiObj = req.get("kimi");
        if (kimiObj instanceof Map) {
            Map<String, String> kimiMap = (Map<String, String>) kimiObj;
            LLMConfig kimi = new LLMConfig();
            kimi.setProvider("kimi");
            kimi.setApiKey(kimiMap.get("apiKey"));
            kimi.setModel(kimiMap.get("model"));
            kimi.setIsActive("kimi".equals(selectedProvider) ? 1 : 0);
            llmConfigMapper.save(kimi);
        }

        // Save minimax config
        Object minimaxObj = req.get("minimax");
        if (minimaxObj instanceof Map) {
            Map<String, String> minimaxMap = (Map<String, String>) minimaxObj;
            LLMConfig minimax = new LLMConfig();
            minimax.setProvider("minimax");
            minimax.setApiKey(minimaxMap.get("apiKey"));
            minimax.setModel(minimaxMap.get("model"));
            minimax.setIsActive("minimax".equals(selectedProvider) ? 1 : 0);
            llmConfigMapper.save(minimax);
        }

        return Result.ok(null);
    }
}
