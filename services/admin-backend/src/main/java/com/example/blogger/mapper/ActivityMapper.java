package com.example.blogger.mapper;

import com.example.blogger.entity.Activity;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ActivityMapper {

    @Select("SELECT id, title, content, qr_code_url as qrCodeUrl, status, sort_order as sortOrder, is_deleted as isDeleted, created_at as createdAt, updated_at as updatedAt FROM tu_activity WHERE is_deleted = 0 ORDER BY sort_order ASC, created_at DESC")
    List<Activity> findAll();

    @Select("SELECT id, title, content, qr_code_url as qrCodeUrl, status, sort_order as sortOrder, is_deleted as isDeleted, created_at as createdAt, updated_at as updatedAt FROM tu_activity WHERE id = #{id} AND is_deleted = 0")
    Activity findById(String id);

    @Insert("INSERT INTO tu_activity(id, title, content, qr_code_url, status, sort_order, is_deleted, created_at, updated_at) VALUES(#{id}, #{title}, #{content}, #{qrCodeUrl}, #{status}, #{sortOrder}, 0, NOW(), NOW())")
    int insert(Activity activity);

    @Update("UPDATE tu_activity SET title=#{title}, content=#{content}, qr_code_url=#{qrCodeUrl}, status=#{status}, sort_order=#{sortOrder}, updated_at=NOW() WHERE id=#{id}")
    int update(Activity activity);

    @Update("UPDATE tu_activity SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);
}
