package com.example.blogger.mapper;

import com.example.blogger.entity.PromptTemplate;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface PromptTemplateMapper {

    @Select("SELECT * FROM tu_prompt_template WHERE is_deleted = 0 ORDER BY created_at DESC")
    List<PromptTemplate> findAll();

    @Select("SELECT * FROM tu_prompt_template WHERE id = #{id} AND is_deleted = 0")
    PromptTemplate findById(String id);

    @Select("SELECT * FROM tu_prompt_template WHERE type = #{type} AND is_default = 1 AND is_deleted = 0 LIMIT 1")
    PromptTemplate findDefaultByType(String type);

    @Select("SELECT * FROM tu_prompt_template WHERE type = #{type} AND is_deleted = 0 ORDER BY created_at DESC LIMIT 1")
    PromptTemplate findLatestByType(String type);

    @Insert("INSERT INTO tu_prompt_template(id, name, content, type, is_default, is_deleted, created_at) " +
            "VALUES(#{id}, #{name}, #{content}, #{type}, #{isDefault}, 0, NOW())")
    int insert(PromptTemplate template);

    @Update("UPDATE tu_prompt_template SET name=#{name}, content=#{content}, type=#{type}, is_default=#{isDefault} WHERE id=#{id}")
    int update(PromptTemplate template);

    @Update("UPDATE tu_prompt_template SET is_default = 0 WHERE type = #{type}")
    int clearDefaultByType(String type);

    @Update("UPDATE tu_prompt_template SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);
}
