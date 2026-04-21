package com.example.user.mapper;

import com.example.user.entity.Guide;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface GuideMapper {
    @Select("SELECT * FROM tu_guide WHERE is_deleted = 0 AND status = '已上架' ORDER BY sort_order ASC, created_at DESC")
    List<Guide> findAll();

    @Select("SELECT * FROM tu_guide WHERE id = #{id} AND is_deleted = 0 AND status = '已上架'")
    Guide findById(String id);
}
