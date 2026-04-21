package com.example.blogger.mapper;

import com.example.blogger.entity.DailyRecommend;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DailyRecommendMapper {
    @Select("SELECT d.*, p.title AS ref_post_title FROM tu_daily_recommend d LEFT JOIN tu_post p ON d.ref_post_id = p.id WHERE d.is_deleted = 0 ORDER BY d.created_at DESC")
    List<DailyRecommend> findAll();

    @Select("SELECT d.*, p.title AS ref_post_title FROM tu_daily_recommend d LEFT JOIN tu_post p ON d.ref_post_id = p.id WHERE d.id = #{id} AND d.is_deleted = 0")
    DailyRecommend findById(String id);

    @Select("SELECT d.*, p.title AS ref_post_title FROM tu_daily_recommend d LEFT JOIN tu_post p ON d.ref_post_id = p.id WHERE d.track_id = #{trackId} AND d.platform = #{platform} AND d.is_deleted = 0 AND d.status = '已上架' ORDER BY d.sort_order ASC")
    List<DailyRecommend> findByTrackAndPlatform(@Param("trackId") String trackId, @Param("platform") String platform);

    @Insert("INSERT INTO tu_daily_recommend(id, track_id, platform, title, summary, ref_post_id, ref_url, sort_order, status, is_deleted) VALUES(#{id}, #{trackId}, #{platform}, #{title}, #{summary}, #{refPostId}, #{refUrl}, #{sortOrder}, #{status}, 0)")
    int insert(DailyRecommend d);

    @Update("UPDATE tu_daily_recommend SET track_id=#{trackId}, platform=#{platform}, title=#{title}, summary=#{summary}, ref_post_id=#{refPostId}, ref_url=#{refUrl}, sort_order=#{sortOrder}, status=#{status} WHERE id=#{id}")
    int update(DailyRecommend d);

    @Update("UPDATE tu_daily_recommend SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);
}
