package com.example.blogger.mapper;

import com.example.blogger.entity.Help;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface HelpMapper {
    @Select("SELECT * FROM tu_help WHERE is_deleted = 0 ORDER BY sort_order ASC, created_at DESC")
    List<Help> findAll();

    @Select("SELECT * FROM tu_help WHERE id = #{id} AND is_deleted = 0")
    Help findById(String id);

    @Insert("INSERT INTO tu_help(id, title, category, content, sort_order, status) VALUES(#{id}, #{title}, #{category}, #{content}, #{sortOrder}, #{status})")
    int insert(Help help);

    @Update("UPDATE tu_help SET title=#{title}, category=#{category}, content=#{content}, sort_order=#{sortOrder}, status=#{status} WHERE id=#{id}")
    int update(Help help);

    @Update("UPDATE tu_help SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);
}
