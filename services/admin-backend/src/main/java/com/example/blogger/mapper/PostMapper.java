package com.example.blogger.mapper;

import com.example.blogger.entity.Post;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface PostMapper {
    @Select("SELECT * FROM tu_post WHERE blogger_id = #{bloggerId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<Post> findByBloggerId(String bloggerId);

    @Select("SELECT * FROM tu_post WHERE is_deleted = 0 ORDER BY created_at DESC")
    List<Post> findAll();

    @Select("<script>SELECT p.* FROM tu_post p INNER JOIN tu_blogger b ON p.blogger_id = b.id WHERE p.is_deleted = 0 " +
            "<if test='platform != null and platform != \"\"'>AND p.platform = #{platform} </if>" +
            "<if test='trackId != null and trackId != \"\"'>AND b.track_id = #{trackId} </if>" +
            "<if test='keyword != null and keyword != \"\"'>AND p.title LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "ORDER BY p.created_at DESC</script>")
    List<Post> search(@Param("platform") String platform, @Param("trackId") String trackId, @Param("keyword") String keyword);

    @Insert("INSERT INTO tu_post(id, title, url, blogger_id, content, platform, summary, tag, `reads`, `likes`, `comments`, metrics_json, status) VALUES(#{id}, #{title}, #{url}, #{bloggerId}, #{content}, #{platform}, #{summary}, #{tag}, #{reads}, #{likes}, #{comments}, #{metricsJson}, #{status})")
    int insert(Post post);

    @Update("UPDATE tu_post SET title=#{title}, url=#{url}, blogger_id=#{bloggerId}, content=#{content}, platform=#{platform}, summary=#{summary}, tag=#{tag}, `reads`=#{reads}, `likes`=#{likes}, `comments`=#{comments}, metrics_json=#{metricsJson}, status=#{status} WHERE id=#{id}")
    int update(Post post);

    @Update("UPDATE tu_post SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);

    @Select("SELECT COUNT(*) FROM tu_post WHERE is_deleted = 0")
    int countPosts();

    @Select("SELECT platform, COUNT(*) as cnt FROM tu_post WHERE is_deleted = 0 AND platform IS NOT NULL AND platform != '' GROUP BY platform ORDER BY cnt DESC")
    List<com.example.blogger.entity.PlatformCount> countByPlatform();
}
