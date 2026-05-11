package com.example.blogger.mapper;

import com.example.blogger.entity.AiFlavorRule;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiFlavorRuleMapper {

    @Select("SELECT id, rule_from, rule_to, sort_order, is_enabled, created_at FROM tu_ai_flavor_rule ORDER BY sort_order ASC, created_at ASC")
    List<AiFlavorRule> findAll();

    @Select("SELECT id, rule_from, rule_to, sort_order, is_enabled, created_at FROM tu_ai_flavor_rule WHERE id = #{id}")
    AiFlavorRule findById(@Param("id") String id);

    @Insert("INSERT INTO tu_ai_flavor_rule(id, rule_from, rule_to, sort_order, is_enabled, created_at) " +
            "VALUES(#{id}, #{ruleFrom}, #{ruleTo}, #{sortOrder}, #{isEnabled}, NOW())")
    int insert(AiFlavorRule rule);

    @Update("UPDATE tu_ai_flavor_rule SET rule_from=#{ruleFrom}, rule_to=#{ruleTo}, sort_order=#{sortOrder}, is_enabled=#{isEnabled} WHERE id=#{id}")
    int update(AiFlavorRule rule);

    @Delete("DELETE FROM tu_ai_flavor_rule WHERE id = #{id}")
    int deleteById(@Param("id") String id);
}