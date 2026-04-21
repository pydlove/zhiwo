package com.example.blogger.mapper;

import com.example.blogger.entity.Track;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TrackMapper {
    @Select("SELECT t.*, " +
            "(SELECT COUNT(*) FROM tu_blogger WHERE track_id = t.id AND is_deleted = 0) as blogger_count, " +
            "(SELECT COUNT(*) FROM tu_post WHERE blogger_id IN (SELECT id FROM tu_blogger WHERE track_id = t.id AND is_deleted = 0) AND is_deleted = 0) as post_count " +
            "FROM tu_track t WHERE t.is_deleted = 0 ORDER BY t.sort_order ASC")
    List<Track> findAll();

    @Select("SELECT * FROM tu_track WHERE id = #{id} AND is_deleted = 0")
    Track findById(String id);

    @Select("SELECT COUNT(*) FROM tu_blogger WHERE track_id = #{trackId} AND is_deleted = 0")
    int countBloggersByTrack(String trackId);

    @Select("SELECT COUNT(*) FROM tu_post WHERE blogger_id IN (SELECT id FROM tu_blogger WHERE track_id = #{trackId} AND is_deleted = 0) AND is_deleted = 0")
    int countPostsByTrack(String trackId);

    @Insert("INSERT INTO tu_track(id, name, icon, sort_order, preview_bloggers, intro, platforms, cover_json) VALUES(#{id}, #{name}, #{icon}, #{sortOrder}, #{previewBloggers}, #{intro}, #{platforms}, #{coverJson})")
    int insert(Track track);

    @Update("UPDATE tu_track SET name=#{name}, icon=#{icon}, sort_order=#{sortOrder}, preview_bloggers=#{previewBloggers}, intro=#{intro}, platforms=#{platforms}, cover_json=#{coverJson} WHERE id=#{id}")
    int update(Track track);

    @Update("UPDATE tu_track SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);

    @Select("SELECT COUNT(*) FROM tu_track WHERE is_deleted = 0")
    int countTracks();

    @Select("SELECT t.*, " +
            "(SELECT COUNT(*) FROM tu_blogger WHERE track_id = t.id AND is_deleted = 0) as blogger_count, " +
            "(SELECT COUNT(*) FROM tu_post WHERE blogger_id IN (SELECT id FROM tu_blogger WHERE track_id = t.id AND is_deleted = 0) AND is_deleted = 0) as post_count " +
            "FROM tu_track t WHERE t.is_deleted = 0 ORDER BY post_count DESC LIMIT #{limit}")
    List<Track> findTopTracks(@Param("limit") int limit);
}
