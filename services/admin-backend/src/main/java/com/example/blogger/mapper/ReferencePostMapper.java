package com.example.blogger.mapper;

import com.example.blogger.entity.ReferencePost;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ReferencePostMapper {
    @Select("SELECT * FROM tu_reference_post WHERE is_deleted = 0 ORDER BY created_at DESC")
    List<ReferencePost> findAll();

    @Select("SELECT * FROM tu_reference_post WHERE id = #{id} AND is_deleted = 0")
    ReferencePost findById(String id);

    @Insert("INSERT INTO tu_reference_post(id, track_id, platform, title, content, url, sort_order, status, is_deleted) VALUES(#{id}, #{trackId}, #{platform}, #{title}, #{content}, #{url}, #{sortOrder}, #{status}, 0)")
    int insert(ReferencePost r);

    @Update("UPDATE tu_reference_post SET track_id=#{trackId}, platform=#{platform}, title=#{title}, content=#{content}, url=#{url}, sort_order=#{sortOrder}, status=#{status} WHERE id=#{id}")
    int update(ReferencePost r);

    @Update("UPDATE tu_reference_post SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);
}
