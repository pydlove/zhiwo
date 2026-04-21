package com.example.blogger.mapper;

import com.example.blogger.entity.HelpCategory;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface HelpCategoryMapper {
    @Select("SELECT * FROM tu_help_category WHERE is_deleted = 0 ORDER BY sort_order ASC, created_at DESC")
    List<HelpCategory> findAll();

    @Select("SELECT * FROM tu_help_category WHERE id = #{id} AND is_deleted = 0")
    HelpCategory findById(String id);

    @Insert("INSERT INTO tu_help_category(id, name, color, sort_order, is_deleted) VALUES(#{id}, #{name}, #{color}, #{sortOrder}, 0)")
    int insert(HelpCategory category);

    @Update("UPDATE tu_help_category SET name=#{name}, color=#{color}, sort_order=#{sortOrder} WHERE id=#{id}")
    int update(HelpCategory category);

    @Update("UPDATE tu_help_category SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);
}
