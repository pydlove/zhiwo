package com.example.user.mapper;

import com.example.user.entity.SubscriptionPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SubscriptionPostMapper {
    @Select("SELECT * FROM tu_subscription_post WHERE user_id = #{userId} AND track_id = #{trackId} AND is_deleted = 0 AND status = '已上架' ORDER BY created_at DESC LIMIT 1")
    SubscriptionPost findLatestByUserAndTrack(@Param("userId") String userId, @Param("trackId") String trackId);

    @Update("UPDATE tu_subscription_post SET used = 1 WHERE id = #{id}")
    int markUsedById(@Param("id") String id);
}
