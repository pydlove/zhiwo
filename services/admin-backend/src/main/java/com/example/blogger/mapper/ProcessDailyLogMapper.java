package com.example.blogger.mapper;

import com.example.blogger.entity.ProcessDailyLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ProcessDailyLogMapper {

    @Insert("INSERT INTO process_daily_log(id, target_date, check_time, status, titles_needed, titles_generated, " +
            "titles_approved, titles_pushed, titles_matched, articles_needed, articles_uploaded, " +
            "push_scheduled_time, push_success, push_failed, error_msg, created_at, updated_at) " +
            "VALUES(#{id}, #{targetDate}, #{checkTime}, #{status}, #{titlesNeeded}, #{titlesGenerated}, " +
            "#{titlesApproved}, #{titlesPushed}, #{titlesMatched}, #{articlesNeeded}, #{articlesUploaded}, " +
            "#{pushScheduledTime}, #{pushSuccess}, #{pushFailed}, #{errorMsg}, NOW(), NOW())")
    int insert(ProcessDailyLog log);

    @Select("SELECT * FROM process_daily_log WHERE target_date = #{targetDate} ORDER BY created_at DESC LIMIT 1")
    ProcessDailyLog findByTargetDate(LocalDate targetDate);

    @Select("SELECT * FROM process_daily_log ORDER BY target_date DESC LIMIT #{limit}")
    List<ProcessDailyLog> findRecent(@Param("limit") int limit);

    @Update("UPDATE process_daily_log SET status=#{status}, titles_needed=#{titlesNeeded}, " +
            "titles_generated=#{titlesGenerated}, titles_approved=#{titlesApproved}, " +
            "titles_pushed=#{titlesPushed}, titles_matched=#{titlesMatched}, " +
            "articles_needed=#{articlesNeeded}, articles_uploaded=#{articlesUploaded}, " +
            "push_scheduled_time=#{pushScheduledTime}, push_success=#{pushSuccess}, " +
            "push_failed=#{pushFailed}, error_msg=#{errorMsg}, updated_at=NOW() WHERE id=#{id}")
    int update(ProcessDailyLog log);
}
