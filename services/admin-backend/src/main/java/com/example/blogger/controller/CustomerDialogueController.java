package com.example.blogger.controller;

import com.example.blogger.entity.CustomerDialogue;
import com.example.blogger.entity.Result;
import com.example.blogger.service.CustomerDialogueService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer-dialogues")
@CrossOrigin(origins = "*")
public class CustomerDialogueController {

    private final CustomerDialogueService customerDialogueService;

    public CustomerDialogueController(CustomerDialogueService customerDialogueService) {
        this.customerDialogueService = customerDialogueService;
    }

    @GetMapping
    public Result<List<CustomerDialogue>> list(@RequestParam(value = "category", required = false) String category) {
        if (category != null && !category.isEmpty()) {
            return Result.ok(customerDialogueService.listByCategory(category));
        }
        return Result.ok(customerDialogueService.list());
    }

    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.ok(customerDialogueService.listCategories());
    }

    @GetMapping("/{id}")
    public Result<CustomerDialogue> get(@PathVariable String id) {
        return Result.ok(customerDialogueService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody CustomerDialogue customerDialogue) {
        customerDialogueService.save(customerDialogue);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        customerDialogueService.delete(id);
        return Result.ok(null);
    }
}
