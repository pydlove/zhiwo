package com.example.blogger.mapper;

import com.example.blogger.entity.AgentExecution;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AgentExecutionMapper {

    @Insert("INSERT INTO tu_agent_execution (execution_date, status, total_users, total_tracks, " +
            "matched_titles, generated_titles, article_tasks, failed_count, detail_json, started_at, error_message) " +
            "VALUES (#{executionDate}, #{status}, #{totalUsers}, #{totalTracks}, #{matchedTitles}, " +
            "#{generatedTitles}, #{articleTasks}, #{failedCount}, #{detailJson}, #{startedAt}, #{errorMessage})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AgentExecution execution);

    @Update("UPDATE tu_agent_execution SET status = #{status}, total_users = #{totalUsers}, " +
            "total_tracks = #{totalTracks}, matched_titles = #{matchedTitles}, " +
            "generated_titles = #{generatedTitles}, article_tasks = #{articleTasks}, " +
            "failed_count = #{failedCount}, detail_json = #{detailJson}, " +
            "completed_at = #{completedAt}, error_message = #{errorMessage} WHERE id = #{id}")
    void update(AgentExecution execution);

    @Select("SELECT id, execution_date as executionDate, status, total_users as totalUsers, " +
            "total_tracks as totalTracks, matched_titles as matchedTitles, " +
            "generated_titles as generatedTitles, article_tasks as articleTasks, " +
            "failed_count as failedCount, detail_json as detailJson, " +
            "started_at as startedAt, completed_at as completedAt, error_message as errorMessage " +
            "FROM tu_agent_execution WHERE id = #{id}")
    AgentExecution findById(Long id);

    @Select("SELECT id, execution_date as executionDate, status, total_users as totalUsers, " +
            "total_tracks as totalTracks, matched_titles as matchedTitles, " +
            "generated_titles as generatedTitles, article_tasks as articleTasks, " +
            "failed_count as failedCount, detail_json as detailJson, " +
            "started_at as startedAt, completed_at as completedAt, error_message as errorMessage " +
            "FROM tu_agent_execution ORDER BY started_at DESC LIMIT #{limit}")
    List<AgentExecution> findRecent(@Param("limit") int limit);

    @Select("SELECT id, execution_date as executionDate, status, total_users as totalUsers, " +
            "total_tracks as totalTracks, matched_titles as matchedTitles, " +
            "generated_titles as generatedTitles, article_tasks as articleTasks, " +
            "failed_count as failedCount, detail_json as detailJson, " +
            "started_at as startedAt, completed_at as completedAt, error_message as errorMessage " +
            "FROM tu_agent_execution WHERE execution_date = #{date} ORDER BY started_at DESC")
    List<AgentExecution> findByDate(@Param("date") String date);
}
