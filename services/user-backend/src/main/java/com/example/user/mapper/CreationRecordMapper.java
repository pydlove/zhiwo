package com.example.user.mapper;

import com.example.user.entity.CreationRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CreationRecordMapper {
    @Select("<script>" +
            "SELECT cr.*, u.username as user_name, t.name as track_name " +
            "FROM tu_creation_record cr " +
            "LEFT JOIN tu_user u ON cr.user_id = u.id " +
            "LEFT JOIN tu_track t ON cr.track_id = t.id " +
            "<where>" +
            "<if test='userId != null and userId != \"\"'> AND cr.user_id = #{userId} </if>" +
            "<if test='trackId != null and trackId != \"\"'> AND cr.track_id = #{trackId} </if>" +
            "<if test='date != null and date != \"\"'> AND DATE(cr.created_at) = #{date} </if>" +
            "</where>" +
            "ORDER BY cr.created_at DESC" +
            "</script>")
    List<CreationRecord> findAll(@Param("userId") String userId, @Param("trackId") String trackId, @Param("date") String date);

    @Select("SELECT cr.*, u.username as user_name, t.name as track_name " +
            "FROM tu_creation_record cr " +
            "LEFT JOIN tu_user u ON cr.user_id = u.id " +
            "LEFT JOIN tu_track t ON cr.track_id = t.id " +
            "WHERE cr.id = #{id}")
    CreationRecord findById(String id);

    @Insert("INSERT INTO tu_creation_record(id, user_id, track_id, title, content, reviewed, mode) " +
            "VALUES(#{id}, #{userId}, #{trackId}, #{title}, #{content}, #{reviewed}, #{mode})")
    int insert(CreationRecord record);

    @Update("UPDATE tu_creation_record SET user_id=#{userId}, track_id=#{trackId}, title=#{title}, content=#{content}, reviewed=#{reviewed}, mode=#{mode} WHERE id=#{id}")
    int update(CreationRecord record);

    @Update("UPDATE tu_creation_record SET reviewed = 1 WHERE id = #{id}")
    int markReviewed(String id);

    @Select("SELECT COUNT(*) FROM tu_creation_record WHERE user_id = #{userId} AND DATE(created_at) = #{date}")
    int countByUserIdAndDate(@Param("userId") String userId, @Param("date") String date);

    @Delete("DELETE FROM tu_creation_record WHERE id = #{id}")
    int delete(String id);
}
