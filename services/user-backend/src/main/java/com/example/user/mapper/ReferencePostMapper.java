package com.example.user.mapper;

import com.example.user.entity.ReferencePost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ReferencePostMapper {
    @Select("SELECT * FROM tu_reference_post WHERE track_id = #{trackId} AND platform = #{platform} AND is_deleted = 0 AND status = '已上架' ORDER BY sort_order ASC")
    List<ReferencePost> findByTrackAndPlatform(@Param("trackId") String trackId, @Param("platform") String platform);
}
