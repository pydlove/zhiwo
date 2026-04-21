package com.example.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StatsMapper {
    @Select("SELECT COUNT(*) FROM tu_post WHERE DATE(created_at) = CURDATE()")
    int countTodayPosts();

    @Select("SELECT COUNT(*) FROM tu_post WHERE DATE(created_at) = CURDATE() - INTERVAL 1 DAY")
    int countYesterdayPosts();

    @Select("SELECT COUNT(*) FROM tu_track")
    int countTracks();

    @Select("SELECT COUNT(*) FROM tu_blogger")
    int countBloggers();

    @Select("SELECT COUNT(*) FROM tu_post")
    int countPosts();

    @Select("SELECT COUNT(*) FROM tu_track WHERE YEARWEEK(created_at, 1) = YEARWEEK(CURDATE(), 1)")
    int countWeekTracks();

    @Select("SELECT COUNT(*) FROM tu_blogger WHERE YEARWEEK(created_at, 1) = YEARWEEK(CURDATE(), 1)")
    int countWeekBloggers();

    @Select("SELECT COUNT(*) FROM tu_post WHERE YEARWEEK(created_at, 1) = YEARWEEK(CURDATE(), 1)")
    int countWeekPosts();

    @Select("SELECT COUNT(*) FROM tu_subscription_post WHERE is_deleted = 0 AND status = '已上架'")
    int countSubscriptionPosts();
}
