package com.example.user.controller;

import com.example.user.entity.CustomerDialogue;
import com.example.user.entity.Result;
import com.example.user.mapper.CustomerDialogueMapper;
import com.example.user.mapper.OperatorMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/operators")
@CrossOrigin(origins = "*")
public class OperatorController {

    private final OperatorMapper operatorMapper;
    private final CustomerDialogueMapper customerDialogueMapper;

    public OperatorController(OperatorMapper operatorMapper, CustomerDialogueMapper customerDialogueMapper) {
        this.operatorMapper = operatorMapper;
        this.customerDialogueMapper = customerDialogueMapper;
    }

    @GetMapping("/{username}")
    public Result<Map<String, String>> getOperatorInfo(@PathVariable String username) {
        if (username == null || username.trim().isEmpty()) {
            return Result.error("运营者标识不能为空");
        }
        Map<String, Object> operator = operatorMapper.findOperatorByUsername(username.trim());
        if (operator == null) {
            return Result.error("运营者不存在");
        }
        Map<String, String> data = new HashMap<>();
        data.put("id", operator.get("id") != null ? operator.get("id").toString() : "");
        data.put("username", operator.get("username") != null ? operator.get("username").toString() : "");
        data.put("name", operator.get("name") != null ? operator.get("name").toString() : "");
        data.put("qrCodeUrl", operator.get("qrCodeUrl") != null ? operator.get("qrCodeUrl").toString() : "");
        return Result.ok(data);
    }

    @GetMapping("/{username}/dialogues")
    public Result<List<CustomerDialogue>> getOperatorDialogues(
            @PathVariable String username,
            @RequestParam(value = "category", required = false) String category) {
        if (username == null || username.trim().isEmpty()) {
            return Result.error("运营者标识不能为空");
        }
        Map<String, Object> operator = operatorMapper.findOperatorByUsername(username.trim());
        if (operator == null) {
            return Result.error("运营者不存在");
        }
        String adminId = operator.get("id") != null ? operator.get("id").toString() : "";
        List<CustomerDialogue> list;
        if (category != null && !category.isEmpty()) {
            list = customerDialogueMapper.findByCategoryAndAdminId(category, adminId);
        } else {
            list = customerDialogueMapper.findByAdminId(adminId);
        }
        return Result.ok(list);
    }
}
