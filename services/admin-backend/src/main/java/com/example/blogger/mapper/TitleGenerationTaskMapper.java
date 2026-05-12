package com.example.blogger.mapper;

import com.example.blogger.entity.TitleGenerationTask;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TitleGenerationTaskMapper {

    @Select("SELECT * FROM tu_title_generation_task WHERE id = #{id}")
    TitleGenerationTask findById(@Param("id") String id);

    @Select("SELECT * FROM tu_title_generation_task WHERE title_library_id = #{titleLibraryId} ORDER BY created_at DESC")
    List<TitleGenerationTask> findByTitleLibraryId(@Param("titleLibraryId") String titleLibraryId);

    @Select("SELECT * FROM tu_title_generation_task WHERE status = 'pending' ORDER BY created_at ASC LIMIT 1")
    TitleGenerationTask findOnePending();

    @Select("SELECT * FROM tu_title_generation_task ORDER BY created_at DESC")
    List<TitleGenerationTask> findAll();

    @Insert("INSERT INTO tu_title_generation_task(id, title_library_id, title, prompt, status, progress_step, progress_message, created_at, updated_at) " +
            "VALUES(#{id}, #{titleLibraryId}, #{title}, #{prompt}, #{status}, #{progressStep}, #{progressMessage}, #{createdAt}, #{updatedAt})")
    int insert(TitleGenerationTask task);

    @Update("UPDATE tu_title_generation_task SET status = #{status}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateStatus(@Param("id") String id, @Param("status") String status, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE tu_title_generation_task SET status = #{status}, result_file_url = #{resultFileUrl}, " +
            "result_file_name = #{resultFileName}, processed_at = #{processedAt}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateCompleted(@Param("id") String id, @Param("status") String status,
                        @Param("resultFileUrl") String resultFileUrl, @Param("resultFileName") String resultFileName,
                        @Param("processedAt") LocalDateTime processedAt, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE tu_title_generation_task SET status = 'failed', error_message = #{errorMessage}, " +
            "updated_at = #{updatedAt} WHERE id = #{id}")
    int updateFailed(@Param("id") String id, @Param("errorMessage") String errorMessage, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE tu_title_generation_task SET progress_step = #{step}, progress_message = #{message}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateProgress(@Param("id") String id, @Param("step") Integer step, @Param("message") String message, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE tu_title_generation_task SET generated_content = #{content}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateGeneratedContent(@Param("id") String id, @Param("content") String content, @Param("updatedAt") LocalDateTime updatedAt);

    @Delete("DELETE FROM tu_title_generation_task WHERE id = #{id} AND status = 'pending'")
    int deletePendingById(@Param("id") String id);

    @Select("SELECT COUNT(*) FROM tu_title_generation_task WHERE status = #{status}")
    int countByStatus(@Param("status") String status);

    @Select("<script>" +
            "SELECT * FROM tu_title_generation_task " +
            "<where>" +
            "<if test=\"keyword != null and keyword != ''\"> AND title LIKE CONCAT('%', #{keyword}, '%') </if>" +
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
            @Result(property = "titleLibraryId", column = "title_library_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "prompt", column = "prompt"),
            @Result(property = "status", column = "status"),
            @Result(property = "resultFileUrl", column = "result_file_url"),
            @Result(property = "resultFileName", column = "result_file_name"),
            @Result(property = "errorMessage", column = "error_message"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "processedAt", column = "processed_at"),
            @Result(property = "progressStep", column = "progress_step"),
            @Result(property = "progressMessage", column = "progress_message")
    })
    List<TitleGenerationTask> findAllWithSearch(@Param("keyword") String keyword, @Param("status") String status);
}
