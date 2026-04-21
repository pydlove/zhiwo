package com.example.user.mapper;

import com.example.user.entity.DailyRecommend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DailyRecommendMapper {
    @Select("SELECT d.*, p.title AS ref_post_title FROM tu_daily_recommend d LEFT JOIN tu_post p ON d.ref_post_id = p.id WHERE d.track_id = #{trackId} AND d.platform = #{platform} AND d.is_deleted = 0 AND d.status = '已上架' ORDER BY d.sort_order ASC")
    List<DailyRecommend> findByTrackAndPlatform(@Param("trackId") String trackId, @Param("platform") String platform);
}
