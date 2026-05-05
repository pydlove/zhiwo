package com.example.blogger.mapper;

import com.example.blogger.entity.CustomerDialogue;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CustomerDialogueMapper {

    @Select("SELECT * FROM tu_customer_dialogue ORDER BY sort_order ASC, created_at DESC")
    List<CustomerDialogue> findAll();

    @Select("SELECT * FROM tu_customer_dialogue WHERE category = #{category} ORDER BY sort_order ASC, created_at DESC")
    List<CustomerDialogue> findByCategory(String category);

    @Select("SELECT DISTINCT category FROM tu_customer_dialogue ORDER BY category")
    List<String> findAllCategories();

    @Select("<script>SELECT * FROM tu_customer_dialogue WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "ORDER BY sort_order ASC, created_at DESC</script>")
    List<CustomerDialogue> findByIds(@Param("ids") List<String> ids);

    @Select("SELECT * FROM tu_customer_dialogue WHERE id = #{id}")
    CustomerDialogue findById(String id);

    @Insert("INSERT INTO tu_customer_dialogue(id, category, question, reply, image_url, sort_order, created_at, updated_at) " +
            "VALUES(#{id}, #{category}, #{question}, #{reply}, #{imageUrl}, #{sortOrder}, NOW(), NOW())")
    int insert(CustomerDialogue customerDialogue);

    @Update("UPDATE tu_customer_dialogue SET category = #{category}, question = #{question}, reply = #{reply}, " +
            "image_url = #{imageUrl}, sort_order = #{sortOrder}, updated_at = NOW() WHERE id = #{id}")
    int update(CustomerDialogue customerDialogue);

    @Delete("DELETE FROM tu_customer_dialogue WHERE id = #{id}")
    int delete(String id);
}
