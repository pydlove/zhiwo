package com.example.blogger.mapper;

import com.example.blogger.entity.Track;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TrackMapper {
    @Select("SELECT * FROM track ORDER BY sort_order ASC")
    List<Track> findAll();

    @Select("SELECT * FROM track WHERE id = #{id}")
    Track findById(String id);

    @Insert("INSERT INTO track(id, name, icon, sort_order, preview_bloggers) VALUES(#{id}, #{name}, #{icon}, #{sortOrder}, #{previewBloggers})")
    int insert(Track track);

    @Update("UPDATE track SET name=#{name}, icon=#{icon}, sort_order=#{sortOrder}, preview_bloggers=#{previewBloggers} WHERE id=#{id}")
    int update(Track track);

    @Delete("DELETE FROM track WHERE id = #{id}")
    int delete(String id);
}
