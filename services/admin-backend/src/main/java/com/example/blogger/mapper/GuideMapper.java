package com.example.blogger.mapper;

import com.example.blogger.entity.Guide;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface GuideMapper {
    @Select("SELECT id, title, category, description, content, link, sort_order as sortOrder, status, is_recommended as isRecommended, is_deleted as isDeleted, created_at as createdAt, updated_at as updatedAt FROM tu_guide WHERE is_deleted = 0 ORDER BY sort_order ASC, created_at DESC")
    List<Guide> findAll();

    @Select("SELECT id, title, category, description, content, link, sort_order as sortOrder, status, is_recommended as isRecommended, is_deleted as isDeleted, created_at as createdAt, updated_at as updatedAt FROM tu_guide WHERE id = #{id} AND is_deleted = 0")
    Guide findById(String id);

    @Insert("INSERT INTO tu_guide(id, title, category, description, content, link, sort_order, status, is_recommended, is_deleted) VALUES(#{id}, #{title}, #{category}, #{description}, #{content}, #{link}, #{sortOrder}, #{status}, #{isRecommended}, 0)")
    int insert(Guide guide);

    @Update("UPDATE tu_guide SET title=#{title}, category=#{category}, description=#{description}, content=#{content}, link=#{link}, sort_order=#{sortOrder}, status=#{status}, is_recommended=#{isRecommended} WHERE id=#{id}")
    int update(Guide guide);

    @Update("UPDATE tu_guide SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);

    @Update("<script>UPDATE tu_guide SET is_recommended = #{isRecommended} WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int batchUpdateRecommended(@Param("ids") List<String> ids, @Param("isRecommended") Integer isRecommended);
}
