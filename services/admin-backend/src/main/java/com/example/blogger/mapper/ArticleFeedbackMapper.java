package com.example.blogger.mapper;

import com.example.blogger.entity.ArticleFeedback;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ArticleFeedbackMapper {

    @Insert("INSERT INTO tu_article_feedback(id, track_id, platform, content, created_at) " +
            "VALUES(#{id}, #{trackId}, #{platform}, #{content}, NOW())")
    int insert(ArticleFeedback feedback);

    @Select("SELECT * FROM tu_article_feedback WHERE id = #{id}")
    ArticleFeedback findById(String id);

    @Select("<script>" +
            "SELECT * FROM tu_article_feedback WHERE 1=1 " +
            "<if test='trackId != null and trackId != \"\"'>AND track_id = #{trackId}</if>" +
            "<if test='platform != null and platform != \"\"'>AND platform = #{platform}</if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    List<ArticleFeedback> findByTrackAndPlatform(@Param("trackId") String trackId, @Param("platform") String platform);

    @Delete("DELETE FROM tu_article_feedback WHERE id = #{id}")
    int deleteById(String id);
}
