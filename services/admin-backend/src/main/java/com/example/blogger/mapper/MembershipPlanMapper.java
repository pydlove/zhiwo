package com.example.blogger.mapper;

import com.example.blogger.entity.MembershipPlan;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MembershipPlanMapper {
    @Select("SELECT * FROM tu_membership_plan ORDER BY sort_order")
    List<MembershipPlan> findAll();

    @Select("SELECT * FROM tu_membership_plan WHERE id = #{id}")
    MembershipPlan findById(String id);

    @Insert("INSERT INTO tu_membership_plan(id, name, price, original_price, features_json, track_limit, ai_limit, platform_limit, expire_days, permissions_json, sort_order, is_active) " +
            "VALUES(#{id}, #{name}, #{price}, #{originalPrice}, #{featuresJson}, #{trackLimit}, #{aiLimit}, #{platformLimit}, #{expireDays}, #{permissionsJson}, #{sortOrder}, #{isActive})")
    int insert(MembershipPlan plan);

    @Update("UPDATE tu_membership_plan SET name=#{name}, price=#{price}, original_price=#{originalPrice}, " +
            "features_json=#{featuresJson}, track_limit=#{trackLimit}, ai_limit=#{aiLimit}, platform_limit=#{platformLimit}, expire_days=#{expireDays}, permissions_json=#{permissionsJson}, sort_order=#{sortOrder}, is_active=#{isActive} WHERE id=#{id}")
    int update(MembershipPlan plan);

    @Delete("DELETE FROM tu_membership_plan WHERE id = #{id}")
    int delete(String id);
}
