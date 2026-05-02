package com.example.blogger.mapper;

import com.example.blogger.entity.TitlePushLog;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TitlePushLogMapper {

    @Insert("INSERT INTO tu_title_push_log(id, title_library_id, server_config_id, title, platform, track_id, status, error_msg, pushed_by, pushed_at) " +
            "VALUES(#{id}, #{titleLibraryId}, #{serverConfigId}, #{title}, #{platform}, #{trackId}, #{status}, #{errorMsg}, #{pushedBy}, NOW())")
    int insert(TitlePushLog titlePushLog);

    @Select("SELECT l.*, sc.name as serverConfigName " +
            "FROM tu_title_push_log l " +
            "LEFT JOIN tu_server_config sc ON l.server_config_id COLLATE utf8mb4_0900_ai_ci = sc.id COLLATE utf8mb4_0900_ai_ci " +
            "WHERE (#{status} IS NULL OR #{status} = '' OR l.status = #{status}) " +
            "ORDER BY l.pushed_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<TitlePushLog> findAll(@Param("status") String status, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM tu_title_push_log WHERE (#{status} IS NULL OR #{status} = '' OR status = #{status})")
    int countAll(@Param("status") String status);

    @Select("SELECT l.*, sc.name as serverConfigName " +
            "FROM tu_title_push_log l " +
            "LEFT JOIN tu_server_config sc ON l.server_config_id COLLATE utf8mb4_0900_ai_ci = sc.id COLLATE utf8mb4_0900_ai_ci " +
            "WHERE l.title_library_id COLLATE utf8mb4_0900_ai_ci = #{titleLibraryId} COLLATE utf8mb4_0900_ai_ci " +
            "ORDER BY l.pushed_at DESC")
    List<TitlePushLog> findByTitleLibraryId(String titleLibraryId);
}
