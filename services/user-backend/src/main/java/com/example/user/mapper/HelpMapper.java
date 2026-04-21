package com.example.user.mapper;

import com.example.user.entity.Help;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface HelpMapper {
    @Select("SELECT * FROM tu_help WHERE is_deleted = 0 AND status = '已上架' ORDER BY sort_order ASC, created_at DESC")
    List<Help> findAll();

    @Select("SELECT * FROM tu_help WHERE id = #{id} AND is_deleted = 0 AND status = '已上架'")
    Help findById(String id);
}
