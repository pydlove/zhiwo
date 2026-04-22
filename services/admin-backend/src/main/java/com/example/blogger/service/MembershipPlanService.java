package com.example.blogger.service;

import com.example.blogger.entity.MembershipPlan;
import com.example.blogger.mapper.MembershipPlanMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class MembershipPlanService {
    private final MembershipPlanMapper membershipPlanMapper;

    public MembershipPlanService(MembershipPlanMapper membershipPlanMapper) {
        this.membershipPlanMapper = membershipPlanMapper;
    }

    public List<MembershipPlan> list() {
        return membershipPlanMapper.findAll();
    }

    public MembershipPlan getById(String id) {
        return membershipPlanMapper.findById(id);
    }

    public void save(MembershipPlan plan) {
        if (plan.getPrice() == null) plan.setPrice(new java.math.BigDecimal("0"));
        if (plan.getOriginalPrice() == null) plan.setOriginalPrice(new java.math.BigDecimal("0"));
        if (plan.getSortOrder() == null) plan.setSortOrder(0);
        if (plan.getIsActive() == null) plan.setIsActive(1);
        if (plan.getFeaturesJson() == null || plan.getFeaturesJson().isEmpty()) {
            plan.setFeaturesJson("[]");
        }
        if (plan.getId() == null || plan.getId().isEmpty()) {
            plan.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            membershipPlanMapper.insert(plan);
        } else {
            membershipPlanMapper.update(plan);
        }
    }

    public void delete(String id) {
        membershipPlanMapper.delete(id);
    }
}
