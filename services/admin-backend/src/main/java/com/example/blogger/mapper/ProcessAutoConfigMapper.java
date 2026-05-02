package com.example.blogger.mapper;

import com.example.blogger.entity.ProcessAutoConfig;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ProcessAutoConfigMapper {

    @Select("SELECT * FROM process_auto_config LIMIT 1")
    ProcessAutoConfig findOne();

    @Update("UPDATE process_auto_config SET check_time=#{checkTime}, check_platforms=#{checkPlatforms}, " +
            "check_all_tracks=#{checkAllTracks}, auto_notify_local=#{autoNotifyLocal}, " +
            "titles_per_track=#{titlesPerTrack}, auto_push_after_approve=#{autoPushAfterApprove}, " +
            "auto_match_after_push=#{autoMatchAfterPush}, is_enabled=#{isEnabled}, updated_at=NOW() " +
            "WHERE id=#{id}")
    int update(ProcessAutoConfig config);
}
