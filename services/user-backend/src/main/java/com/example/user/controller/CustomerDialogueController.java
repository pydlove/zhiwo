package com.example.user.controller;

import com.example.user.entity.CustomerDialogue;
import com.example.user.entity.Result;
import com.example.user.mapper.CustomerDialogueMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-dialogues")
@CrossOrigin(origins = "*")
public class CustomerDialogueController {

    private final CustomerDialogueMapper customerDialogueMapper;

    public CustomerDialogueController(CustomerDialogueMapper customerDialogueMapper) {
        this.customerDialogueMapper = customerDialogueMapper;
    }

    @GetMapping
    public Result<List<CustomerDialogue>> list(@RequestParam(value = "category", required = false) String category) {
        if (category != null && !category.isEmpty()) {
            return Result.ok(customerDialogueMapper.findByCategory(category));
        }
        return Result.ok(customerDialogueMapper.findAll());
    }

    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.ok(customerDialogueMapper.findAllCategories());
    }
}
