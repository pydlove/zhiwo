package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.MembershipPlan;
import com.example.blogger.service.MembershipPlanService;
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
        return Result.ok(membershipPlanService.list());
    }

    @GetMapping("/{id}")
    public Result<MembershipPlan> get(@PathVariable String id) {
        return Result.ok(membershipPlanService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody MembershipPlan plan) {
        membershipPlanService.save(plan);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody MembershipPlan plan) {
        MembershipPlan existing = membershipPlanService.getById(id);
        if (existing == null) {
            return Result.error("套餐不存在");
        }
        plan.setId(id);
        membershipPlanService.save(plan);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        membershipPlanService.delete(id);
        return Result.ok(null);
    }
}
