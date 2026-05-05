package com.example.user.controller;

import com.example.user.entity.CustomerDialogue;
import com.example.user.entity.Result;
import com.example.user.mapper.CustomerDialogueMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @PostMapping
    public Result<Void> save(@RequestBody CustomerDialogue customerDialogue) {
        if (customerDialogue.getId() == null || customerDialogue.getId().isEmpty()) {
            customerDialogue.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        if (customerDialogue.getSortOrder() == null || customerDialogue.getSortOrder() == 0) {
            int nextSort = getNextSortOrder(customerDialogue.getCategory());
            customerDialogue.setSortOrder(nextSort);
        }
        customerDialogueMapper.insert(customerDialogue);
        return Result.ok(null);
    }

    private int getNextSortOrder(String category) {
        if (category == null || category.isEmpty()) {
            return 0;
        }
        List<CustomerDialogue> list = customerDialogueMapper.findByCategory(category);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int max = 0;
        for (CustomerDialogue item : list) {
            int so = item.getSortOrder() != null ? item.getSortOrder() : 0;
            if (so > max) {
                max = so;
            }
        }
        return max + 1;
    }
}
