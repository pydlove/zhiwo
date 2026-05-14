package com.example.blogger.mapper;

import com.example.blogger.entity.Announcement;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AnnouncementMapper {

    @Insert("INSERT INTO tu_announcement(id, type, content, is_enabled, is_deleted, created_at) " +
            "VALUES(#{id}, #{type}, #{content}, #{isEnabled}, 0, NOW())")
    int insert(Announcement announcement);

    @Update("UPDATE tu_announcement SET type=#{type}, content=#{content}, is_enabled=#{isEnabled} WHERE id=#{id}")
    int update(Announcement announcement);

    @Update("UPDATE tu_announcement SET is_deleted=1 WHERE id=#{id}")
    int softDelete(String id);

    @Select("SELECT * FROM tu_announcement WHERE is_deleted = 0 ORDER BY created_at DESC")
    List<Announcement> findAll();

    @Select("SELECT * FROM tu_announcement WHERE id = #{id} AND is_deleted = 0")
    Announcement findById(String id);

    @Select("SELECT * FROM tu_announcement WHERE type = #{type} AND is_deleted = 0 LIMIT 1")
    Announcement findByType(String type);

    @Select("SELECT * FROM tu_announcement WHERE type = #{type} AND is_enabled = 1 AND is_deleted = 0 LIMIT 1")
    Announcement findActiveByType(String type);
}
