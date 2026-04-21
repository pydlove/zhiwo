package com.example.user.service;

import com.example.user.entity.MembershipPlan;
import com.example.user.mapper.MembershipPlanMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MembershipPlanService {
    private final MembershipPlanMapper membershipPlanMapper;

    public MembershipPlanService(MembershipPlanMapper membershipPlanMapper) {
        this.membershipPlanMapper = membershipPlanMapper;
    }

    public List<MembershipPlan> listActive() {
        return membershipPlanMapper.findAllActive();
    }

    public MembershipPlan getById(String id) {
        return membershipPlanMapper.findById(id);
    }
}
