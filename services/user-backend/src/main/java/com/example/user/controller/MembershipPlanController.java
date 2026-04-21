package com.example.user.controller;

import com.example.user.entity.Result;
import com.example.user.entity.MembershipPlan;
import com.example.user.service.MembershipPlanService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/membership-plans")
@CrossOrigin(origins = "*")
public class MembershipPlanController {
    private final MembershipPlanService membershipPlanService;

    public MembershipPlanController(MembershipPlanService membershipPlanService) {
        this.membershipPlanService = membershipPlanService;
    }

    @GetMapping
    public Result<List<MembershipPlan>> list() {
        return Result.ok(membershipPlanService.listActive());
    }

    @GetMapping("/{id}")
    public Result<MembershipPlan> get(@PathVariable String id) {
        return Result.ok(membershipPlanService.getById(id));
    }
}
