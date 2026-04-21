package com.example.user.mapper;

import com.example.user.entity.Style;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface StyleMapper {
    @Select("SELECT * FROM tu_style WHERE is_deleted = 0 AND status = '已启用' ORDER BY created_at DESC")
    List<Style> findAllEnabled();

    @Select("SELECT * FROM tu_style WHERE id = #{id} AND is_deleted = 0")
    Style findById(String id);
}
