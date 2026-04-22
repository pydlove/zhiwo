package com.example.user.mapper;

import com.example.user.entity.Guide;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface GuideMapper {
    @Select("SELECT id, title, category, description, content, link, sort_order as sortOrder, status, is_recommended as isRecommended, is_deleted as isDeleted, created_at as createdAt, updated_at as updatedAt FROM tu_guide WHERE is_deleted = 0 AND status = '已上架' ORDER BY sort_order ASC, created_at DESC")
    List<Guide> findAll();

    @Select("SELECT id, title, category, description, content, link, sort_order as sortOrder, status, is_recommended as isRecommended, is_deleted as isDeleted, created_at as createdAt, updated_at as updatedAt FROM tu_guide WHERE id = #{id} AND is_deleted = 0 AND status = '已上架'")
    Guide findById(String id);

    @Select("SELECT id, title, category, description, content, link, sort_order as sortOrder, status, is_recommended as isRecommended, is_deleted as isDeleted, created_at as createdAt, updated_at as updatedAt FROM tu_guide WHERE is_deleted = 0 AND status = '已上架' AND is_recommended = 1 ORDER BY updated_at DESC")
    List<Guide> findRecommended();
}
