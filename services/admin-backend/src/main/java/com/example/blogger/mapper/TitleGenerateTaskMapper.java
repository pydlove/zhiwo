package com.example.blogger.mapper;

import com.example.blogger.entity.TitleGenerateTask;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TitleGenerateTaskMapper {

    @Select("SELECT * FROM tu_title_generate_task WHERE id = #{id}")
    TitleGenerateTask findById(@Param("id") String id);

    @Select("SELECT * FROM tu_title_generate_task WHERE status = 'pending' ORDER BY created_at ASC LIMIT 1")
    TitleGenerateTask findOnePending();

    @Select("<script>" +
            "SELECT * FROM tu_title_generate_task " +
            "<where>" +
            "<if test=\"status != null and status != ''\"> AND status = #{status} </if>" +
            "</where>" +
            "ORDER BY " +
            "  CASE status " +
            "    WHEN 'processing' THEN 1 " +
            "    WHEN 'pending' THEN 2 " +
            "    WHEN 'completed' THEN 3 " +
            "    WHEN 'failed' THEN 4 " +
            "    ELSE 5 " +
            "  END ASC, " +
            "  created_at DESC" +
            "</script>")
    @Results(id = "taskResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "status", column = "status"),
            @Result(property = "platforms", column = "platforms"),
            @Result(property = "trackIds", column = "track_ids"),
            @Result(property = "countPerCombo", column = "count_per_combo"),
            @Result(property = "instruction", column = "instruction"),
            @Result(property = "resultFileUrl", column = "result_file_url"),
            @Result(property = "resultFileName", column = "result_file_name"),
            @Result(property = "errorMessage", column = "error_message"),
            @Result(property = "progressStep", column = "progress_step"),
            @Result(property = "progressMessage", column = "progress_message"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "processedAt", column = "processed_at"),
            @Result(property = "duplicateCount", column = "duplicate_count"),
            @Result(property = "insertedCount", column = "inserted_count")
    })
    List<TitleGenerateTask> findAllWithSearch(@Param("status") String status);

    @Insert("INSERT INTO tu_title_generate_task(id, status, platforms, track_ids, count_per_combo, instruction, result_file_url, result_file_name, error_message, progress_step, progress_message, duplicate_count, inserted_count, created_at, updated_at) " +
            "VALUES(#{id}, #{status}, #{platforms}, #{trackIds}, #{countPerCombo}, #{instruction}, #{resultFileUrl}, #{resultFileName}, #{errorMessage}, #{progressStep}, #{progressMessage}, #{duplicateCount}, #{insertedCount}, #{createdAt}, #{updatedAt})")
    int insert(TitleGenerateTask task);

    @Update("UPDATE tu_title_generate_task SET status = #{status}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateStatus(@Param("id") String id, @Param("status") String status, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE tu_title_generate_task SET status = #{status}, result_file_url = #{resultFileUrl}, result_file_name = #{resultFileName}, processed_at = #{processedAt}, duplicate_count = #{duplicateCount}, inserted_count = #{insertedCount}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateCompleted(@Param("id") String id, @Param("status") String status,
                        @Param("resultFileUrl") String resultFileUrl, @Param("resultFileName") String resultFileName,
                        @Param("processedAt") LocalDateTime processedAt, @Param("updatedAt") LocalDateTime updatedAt,
                        @Param("duplicateCount") Integer duplicateCount, @Param("insertedCount") Integer insertedCount);

    @Update("UPDATE tu_title_generate_task SET status = 'failed', error_message = #{errorMessage}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateFailed(@Param("id") String id, @Param("errorMessage") String errorMessage, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE tu_title_generate_task SET progress_step = #{step}, progress_message = #{message}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateProgress(@Param("id") String id, @Param("step") Integer step, @Param("message") String message, @Param("updatedAt") LocalDateTime updatedAt);

    @Delete("DELETE FROM tu_title_generate_task WHERE id = #{id} AND status = 'pending'")
    int deletePendingById(@Param("id") String id);

    @Select("SELECT COUNT(*) FROM tu_title_generate_task WHERE status = #{status}")
    int countByStatus(@Param("status") String status);
}
