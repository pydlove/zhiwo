package com.example.user.mapper;

import com.example.user.entity.Post;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface PostMapper {
    @Select("SELECT * FROM tu_post WHERE blogger_id = #{bloggerId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<Post> findByBloggerId(String bloggerId);

    @Select("SELECT p.* FROM tu_post p INNER JOIN tu_blogger b ON p.blogger_id = b.id WHERE b.track_id = #{trackId} AND p.is_deleted = 0 AND b.is_deleted = 0 ORDER BY p.created_at DESC")
    List<Post> findByTrackId(String trackId);

    @Select("<script>SELECT p.* FROM tu_post p INNER JOIN tu_blogger b ON p.blogger_id = b.id WHERE p.is_deleted = 0 AND b.is_deleted = 0 " +
            "<if test='trackId != null and trackId != \"\"'>AND b.track_id = #{trackId} </if>" +
            "<if test='platform != null and platform != \"\"'>AND p.platform = #{platform} </if>" +
            "<if test='keyword != null and keyword != \"\"'>AND p.title LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "ORDER BY p.created_at DESC</script>")
    List<Post> search(@Param("trackId") String trackId, @Param("platform") String platform, @Param("keyword") String keyword);

    @Insert("INSERT INTO tu_post(id, title, url, blogger_id, content, platform, summary, tag, `reads`, `likes`, `comments`, metrics_json, status) VALUES(#{id}, #{title}, #{url}, #{bloggerId}, #{content}, #{platform}, #{summary}, #{tag}, #{reads}, #{likes}, #{comments}, #{metricsJson}, #{status})")
    int insert(Post post);

    @Update("UPDATE tu_post SET title=#{title}, url=#{url}, blogger_id=#{bloggerId}, content=#{content}, platform=#{platform}, summary=#{summary}, tag=#{tag}, `reads`=#{reads}, `likes`=#{likes}, `comments`=#{comments}, metrics_json=#{metricsJson}, status=#{status} WHERE id=#{id}")
    int update(Post post);

    @Update("UPDATE tu_post SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);

    @Select("SELECT p.*, t.name as trackName, t.id as trackId, b.name as bloggerName, b.id as bloggerId FROM tu_post p INNER JOIN tu_blogger b ON p.blogger_id = b.id INNER JOIN tu_track t ON b.track_id = t.id WHERE p.is_deleted = 0 AND b.is_deleted = 0 AND t.is_deleted = 0 ORDER BY p.created_at DESC LIMIT #{limit}")
    List<Map<String, Object>> findRecommendations(int limit);

    @Select("SELECT p.*, t.name as trackName, t.id as trackId, b.name as bloggerName, b.id as bloggerId FROM tu_post p INNER JOIN tu_blogger b ON p.blogger_id = b.id INNER JOIN tu_track t ON b.track_id = t.id WHERE p.is_deleted = 0 AND b.is_deleted = 0 AND t.is_deleted = 0 AND t.id = #{trackId} ORDER BY p.created_at DESC LIMIT 6")
    List<Map<String, Object>> findRecommendationsByTrack(String trackId);
}
