package com.example.user.mapper;

import com.example.user.entity.Track;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TrackMapper {
    @Select("SELECT t.*, " +
            "(SELECT COUNT(*) FROM tu_blogger WHERE track_id = t.id AND is_deleted = 0) as blogger_count, " +
            "(SELECT COUNT(*) FROM tu_post WHERE blogger_id IN (SELECT id FROM tu_blogger WHERE track_id = t.id AND is_deleted = 0) AND is_deleted = 0) as post_count " +
            "FROM tu_track t WHERE t.is_deleted = 0 ORDER BY t.is_hot DESC, t.sort_order ASC")
    List<Track> findAll();

    @Select("SELECT * FROM tu_track WHERE id = #{id} AND is_deleted = 0")
    Track findById(String id);

    @Insert("INSERT INTO tu_track(id, name, icon, sort_order, preview_bloggers, intro, platforms, cover_json, is_hot) VALUES(#{id}, #{name}, #{icon}, #{sortOrder}, #{previewBloggers}, #{intro}, #{platforms}, #{coverJson}, #{isHot})")
    int insert(Track track);

    @Update("UPDATE tu_track SET name=#{name}, icon=#{icon}, sort_order=#{sortOrder}, preview_bloggers=#{previewBloggers}, intro=#{intro}, platforms=#{platforms}, cover_json=#{coverJson}, is_hot=#{isHot} WHERE id=#{id}")
    int update(Track track);

    @Update("UPDATE tu_track SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);
}
