package com.example.user.mapper;

import com.example.user.entity.Blogger;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface BloggerMapper {
    @Select("SELECT * FROM tu_blogger WHERE track_id = #{trackId} AND is_deleted = 0 ORDER BY rank_num ASC")
    List<Blogger> findByTrackId(String trackId);

    @Select("SELECT * FROM tu_blogger WHERE id = #{id} AND is_deleted = 0")
    Blogger findById(String id);

    @Insert("INSERT INTO tu_blogger(id, name, avatar, tagline, track_id, rank_num, link, platform) VALUES(#{id}, #{name}, #{avatar}, #{tagline}, #{trackId}, #{rankNum}, #{link}, #{platform})")
    int insert(Blogger blogger);

    @Update("UPDATE tu_blogger SET name=#{name}, avatar=#{avatar}, tagline=#{tagline}, track_id=#{trackId}, rank_num=#{rankNum}, link=#{link}, platform=#{platform} WHERE id=#{id}")
    int update(Blogger blogger);

    @Update("UPDATE tu_blogger SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);
}
