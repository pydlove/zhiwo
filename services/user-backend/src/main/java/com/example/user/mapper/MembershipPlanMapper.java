package com.example.user.mapper;

import com.example.user.entity.MembershipPlan;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MembershipPlanMapper {
    @Select("SELECT * FROM tu_membership_plan WHERE is_active = 1 ORDER BY sort_order")
    List<MembershipPlan> findAllActive();

    @Select("SELECT * FROM tu_membership_plan WHERE id = #{id}")
    MembershipPlan findById(String id);

    @Select("SELECT * FROM tu_membership_plan WHERE id = #{id} AND is_active = 1")
    MembershipPlan findActiveById(String id);
}
