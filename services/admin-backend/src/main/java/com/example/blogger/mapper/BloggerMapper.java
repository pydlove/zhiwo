package com.example.blogger.mapper;

import com.example.blogger.entity.Blogger;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface BloggerMapper {
    @Select("SELECT b.*, (SELECT COUNT(*) FROM tu_post WHERE blogger_id = b.id AND is_deleted = 0) as article_count FROM tu_blogger b WHERE b.track_id = #{trackId} AND b.is_deleted = 0 ORDER BY b.rank_num ASC")
    List<Blogger> findByTrackId(String trackId);

    @Select("SELECT b.*, (SELECT COUNT(*) FROM tu_post WHERE blogger_id = b.id AND is_deleted = 0) as article_count FROM tu_blogger b WHERE b.is_deleted = 0 ORDER BY b.rank_num ASC")
    List<Blogger> findAll();

    @Select("SELECT * FROM tu_blogger WHERE id = #{id} AND is_deleted = 0")
    Blogger findById(String id);

    @Insert("INSERT INTO tu_blogger(id, name, avatar, tagline, track_id, rank_num, link, platform) VALUES(#{id}, #{name}, #{avatar}, #{tagline}, #{trackId}, #{rankNum}, #{link}, #{platform})")
    int insert(Blogger blogger);

    @Update("UPDATE tu_blogger SET name=#{name}, avatar=#{avatar}, tagline=#{tagline}, track_id=#{trackId}, rank_num=#{rankNum}, link=#{link}, platform=#{platform} WHERE id=#{id}")
    int update(Blogger blogger);

    @Update("UPDATE tu_blogger SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);

    @Select("SELECT COUNT(*) FROM tu_blogger WHERE is_deleted = 0")
    int countBloggers();
}
