package com.example.user.mapper;

import com.example.user.entity.CustomerDialogue;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CustomerDialogueMapper {

    @Select("SELECT * FROM tu_customer_dialogue ORDER BY sort_order ASC, created_at DESC")
    List<CustomerDialogue> findAll();

    @Select("SELECT * FROM tu_customer_dialogue WHERE category = #{category} ORDER BY sort_order ASC, created_at DESC")
    List<CustomerDialogue> findByCategory(@Param("category") String category);

    @Select("SELECT DISTINCT category FROM tu_customer_dialogue ORDER BY category")
    List<String> findAllCategories();
}
