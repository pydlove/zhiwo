package com.example.blogger.mapper;

import com.example.blogger.entity.WritingStyle;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WritingStyleMapper {

    @Select("SELECT * FROM tu_writing_style ORDER BY created_at DESC")
    List<WritingStyle> findAll();

    @Select("SELECT * FROM tu_writing_style WHERE id = #{id}")
    WritingStyle findById(String id);

    @Select("SELECT * FROM tu_writing_style WHERE category = #{category} ORDER BY created_at DESC")
    List<WritingStyle> findByCategory(String category);

    @Select("SELECT DISTINCT category FROM tu_writing_style WHERE category IS NOT NULL AND category != '' ORDER BY category")
    List<String> findAllCategories();

    @Insert("INSERT INTO tu_writing_style(id, original_word, style_word, category, is_active, created_at) " +
            "VALUES(#{id}, #{originalWord}, #{styleWord}, #{category}, #{isActive}, NOW())")
    int insert(WritingStyle writingStyle);

    @Update("UPDATE tu_writing_style SET original_word = #{originalWord}, style_word = #{styleWord}, " +
            "category = #{category}, is_active = #{isActive} WHERE id = #{id}")
    int update(WritingStyle writingStyle);

    @Delete("DELETE FROM tu_writing_style WHERE id = #{id}")
    int delete(String id);
}
