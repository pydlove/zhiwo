package com.example.blogger.mapper;

import com.example.blogger.entity.Style;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface StyleMapper {
    @Select("SELECT * FROM tu_style WHERE is_deleted = 0 ORDER BY created_at DESC")
    List<Style> findAll();

    @Select("SELECT * FROM tu_style WHERE id = #{id} AND is_deleted = 0")
    Style findById(String id);

    @Select("SELECT * FROM tu_style WHERE is_default = 1 AND is_deleted = 0 LIMIT 1")
    Style findDefault();

    @Insert("INSERT INTO tu_style(id, name, scene, is_default, status, style_json, is_deleted) VALUES(#{id}, #{name}, #{scene}, #{isDefault}, #{status}, #{styleJson}, 0)")
    int insert(Style style);

    @Update("UPDATE tu_style SET name=#{name}, scene=#{scene}, is_default=#{isDefault}, status=#{status}, style_json=#{styleJson} WHERE id=#{id}")
    int update(Style style);

    @Update("UPDATE tu_style SET is_default = 0 WHERE is_default = 1")
    int clearDefault();

    @Update("UPDATE tu_style SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);
}
