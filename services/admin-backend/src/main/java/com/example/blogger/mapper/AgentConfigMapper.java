package com.example.blogger.mapper;

import com.example.blogger.entity.AgentConfig;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AgentConfigMapper {

    @Select("SELECT id, enabled, cron_expr as cronExpr, similarity_threshold as similarityThreshold, " +
            "homogeneity_threshold as homogeneityThreshold, min_titles_per_track as minTitlesPerTrack, " +
            "history_days as historyDays, candidate_limit as candidateLimit, " +
            "max_generation_concurrency as maxGenerationConcurrency, created_at as createdAt, updated_at as updatedAt " +
            "FROM tu_agent_config WHERE id = 1")
    AgentConfig findOne();

    @Insert("INSERT INTO tu_agent_config (id, enabled, cron_expr, similarity_threshold, homogeneity_threshold, " +
            "min_titles_per_track, history_days, candidate_limit, max_generation_concurrency) " +
            "VALUES (1, #{enabled}, #{cronExpr}, #{similarityThreshold}, #{homogeneityThreshold}, " +
            "#{minTitlesPerTrack}, #{historyDays}, #{candidateLimit}, #{maxGenerationConcurrency}) " +
            "ON DUPLICATE KEY UPDATE " +
            "enabled = #{enabled}, cron_expr = #{cronExpr}, similarity_threshold = #{similarityThreshold}, " +
            "homogeneity_threshold = #{homogeneityThreshold}, min_titles_per_track = #{minTitlesPerTrack}, " +
            "history_days = #{historyDays}, candidate_limit = #{candidateLimit}, " +
            "max_generation_concurrency = #{maxGenerationConcurrency}")
    void save(AgentConfig config);
}
