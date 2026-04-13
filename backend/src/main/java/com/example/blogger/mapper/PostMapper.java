package com.example.blogger.mapper;

import com.example.blogger.entity.Post;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface PostMapper {
    @Select("SELECT * FROM post WHERE blogger_id = #{bloggerId} ORDER BY created_at DESC")
    List<Post> findByBloggerId(String bloggerId);

    @Insert("INSERT INTO post(id, title, url, blogger_id) VALUES(#{id}, #{title}, #{url}, #{bloggerId})")
    int insert(Post post);

    @Update("UPDATE post SET title=#{title}, url=#{url}, blogger_id=#{bloggerId} WHERE id=#{id}")
    int update(Post post);

    @Delete("DELETE FROM post WHERE id = #{id}")
    int delete(String id);
}
