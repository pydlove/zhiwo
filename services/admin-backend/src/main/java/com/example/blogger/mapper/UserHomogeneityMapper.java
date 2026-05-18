package com.example.blogger.mapper;

import com.example.blogger.entity.UserHomogeneity;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UserHomogeneityMapper {

    @Insert("INSERT INTO tu_user_homogeneity(id, user_id, homogeneity_score, history_count, calculated_at, created_at) " +
            "VALUES(#{id}, #{userId}, #{homogeneityScore}, #{historyCount}, #{calculatedAt}, NOW())")
    int insert(UserHomogeneity record);

    @Update("UPDATE tu_user_homogeneity SET homogeneity_score=#{homogeneityScore}, history_count=#{historyCount}, calculated_at=#{calculatedAt}, updated_at=NOW() WHERE user_id=#{userId}")
    int updateByUserId(UserHomogeneity record);

    @Select("SELECT id, user_id as userId, homogeneity_score as homogeneityScore, history_count as historyCount, calculated_at as calculatedAt, created_at as createdAt, updated_at as updatedAt FROM tu_user_homogeneity WHERE user_id = #{userId}")
    UserHomogeneity findByUserId(String userId);

    @Select("SELECT id, user_id as userId, homogeneity_score as homogeneityScore, history_count as historyCount, calculated_at as calculatedAt, created_at as createdAt, updated_at as updatedAt FROM tu_user_homogeneity")
    List<UserHomogeneity> findAll();
}
