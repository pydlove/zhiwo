package com.example.blogger.mapper;

import com.example.blogger.entity.UserTrack;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UserTrackMapper {
    @Select("SELECT * FROM tu_user_track WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<UserTrack> findByUserId(String userId);

    @Insert("INSERT INTO tu_user_track(user_id, track_id, created_at) VALUES(#{userId}, #{trackId}, NOW())")
    int insert(UserTrack userTrack);

    @Delete("DELETE FROM tu_user_track WHERE user_id = #{userId} AND track_id = #{trackId}")
    int delete(@Param("userId") String userId, @Param("trackId") String trackId);

    @Select("SELECT COUNT(*) FROM tu_user_track WHERE user_id = #{userId}")
    int countByUserId(String userId);
}
