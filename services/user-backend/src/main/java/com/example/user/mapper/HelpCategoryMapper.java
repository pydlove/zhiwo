package com.example.user.mapper;

import com.example.user.entity.HelpCategory;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface HelpCategoryMapper {
    @Select("SELECT * FROM tu_help_category WHERE is_deleted = 0 ORDER BY sort_order ASC, created_at DESC")
    List<HelpCategory> findAll();
}
