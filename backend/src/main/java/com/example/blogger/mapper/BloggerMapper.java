package com.example.blogger.mapper;

import com.example.blogger.entity.Blogger;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface BloggerMapper {
    @Select("SELECT * FROM blogger WHERE track_id = #{trackId} ORDER BY rank_num ASC")
    List<Blogger> findByTrackId(String trackId);

    @Select("SELECT * FROM blogger WHERE id = #{id}")
    Blogger findById(String id);

    @Insert("INSERT INTO blogger(id, name, avatar, tagline, track_id, rank_num) VALUES(#{id}, #{name}, #{avatar}, #{tagline}, #{trackId}, #{rankNum})")
    int insert(Blogger blogger);

    @Update("UPDATE blogger SET name=#{name}, avatar=#{avatar}, tagline=#{tagline}, track_id=#{trackId}, rank_num=#{rankNum} WHERE id=#{id}")
    int update(Blogger blogger);

    @Delete("DELETE FROM blogger WHERE id = #{id}")
    int delete(String id);
}
