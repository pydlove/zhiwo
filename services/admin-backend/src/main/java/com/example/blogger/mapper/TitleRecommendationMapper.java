package com.example.blogger.mapper;

import com.example.blogger.entity.TitleRecommendation;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface TitleRecommendationMapper {

    @Insert("INSERT INTO tu_title_recommendation(id, title_library_id, user_id, platform, track_id, recommend_date, created_at) " +
            "VALUES(#{id}, #{titleLibraryId}, #{userId}, #{platform}, #{trackId}, #{recommendDate}, NOW())")
    int insert(TitleRecommendation recommendation);

    @Select("SELECT r.*, u.username as userName, u.template as userTemplate " +
            "FROM tu_title_recommendation r " +
            "LEFT JOIN tu_user u ON r.user_id = u.id AND u.is_deleted = 0 " +
            "WHERE r.title_library_id = #{titleLibraryId} " +
            "ORDER BY r.recommend_date DESC, r.created_at DESC " +
            "LIMIT 1")
    TitleRecommendation findLatestByTitleId(String titleLibraryId);

    @Select("SELECT COUNT(*) FROM tu_title_recommendation " +
            "WHERE title_library_id = #{titleLibraryId} AND recommend_date = #{date}")
    int countByTitleAndDate(@Param("titleLibraryId") String titleLibraryId, @Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM tu_title_recommendation " +
            "WHERE user_id = #{userId} AND recommend_date = #{date}")
    int countByUserAndDate(@Param("userId") String userId, @Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM tu_title_recommendation " +
            "WHERE user_id = #{userId} AND track_id = #{trackId} AND recommend_date = #{date}")
    int countByUserTrackDate(@Param("userId") String userId, @Param("trackId") String trackId, @Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM tu_title_recommendation " +
            "WHERE user_id = #{userId} AND recommend_date = #{date} " +
            "AND subscription_post_id IS NOT NULL AND subscription_post_id != ''")
    int countRecommendedByUserAndDate(@Param("userId") String userId, @Param("date") LocalDate date);

    @Select("<script>" +
            "SELECT COUNT(*) FROM tu_title_recommendation " +
            "WHERE user_id = #{userId} AND recommend_date = #{date} " +
            "<if test=\"platform != null and platform != ''\">AND platform = #{platform}</if> " +
            "<if test=\"platform == null or platform == ''\">AND (platform IS NULL OR platform = '')</if> " +
            "<if test=\"trackId != null and trackId != ''\">AND track_id = #{trackId}</if> " +
            "<if test=\"trackId == null or trackId == ''\">AND (track_id IS NULL OR track_id = '')</if> " +
            "</script>")
    int countByUserPlatformTrackDate(@Param("userId") String userId,
                                     @Param("platform") String platform,
                                     @Param("trackId") String trackId,
                                     @Param("date") LocalDate date);

    @Delete("DELETE FROM tu_title_recommendation WHERE title_library_id = #{titleLibraryId}")
    int deleteByTitleId(String titleLibraryId);

    @Select("SELECT r.*, u.username as userName, u.template as userTemplate " +
            "FROM tu_title_recommendation r " +
            "LEFT JOIN tu_user u ON r.user_id = u.id AND u.is_deleted = 0 " +
            "WHERE (r.subscription_post_id IS NULL OR r.subscription_post_id = '') " +
            "ORDER BY r.created_at DESC")
    List<TitleRecommendation> findTodayRecommendationsWithoutPost(@Param("date") LocalDate date);

    @Update("UPDATE tu_title_recommendation SET subscription_post_id = #{subscriptionPostId} WHERE id = #{id}")
    int updateSubscriptionPostId(@Param("id") String id, @Param("subscriptionPostId") String subscriptionPostId);

    @Update("UPDATE tu_title_recommendation SET subscription_post_id = NULL WHERE subscription_post_id = #{subscriptionPostId}")
    int clearSubscriptionPostId(@Param("subscriptionPostId") String subscriptionPostId);

    @Select("SELECT r.*, u.username as userName, u.template as userTemplate " +
            "FROM tu_title_recommendation r " +
            "LEFT JOIN tu_user u ON r.user_id = u.id AND u.is_deleted = 0 " +
            "WHERE r.user_id = #{userId} AND r.recommend_date = #{date} " +
            "AND r.subscription_post_id IS NOT NULL AND r.subscription_post_id != '' " +
            "ORDER BY r.created_at DESC " +
            "LIMIT 1")
    TitleRecommendation findLatestByUserAndDate(@Param("userId") String userId, @Param("date") LocalDate date);

    @Select("SELECT DISTINCT track_id FROM tu_title_recommendation " +
            "WHERE user_id = #{userId} AND recommend_date = #{date} " +
            "AND subscription_post_id IS NOT NULL AND subscription_post_id != ''")
    List<String> findRecommendedTrackIdsByUserAndDate(@Param("userId") String userId, @Param("date") LocalDate date);

    @Select("SELECT r.*, t.title as titleLibraryTitle " +
            "FROM tu_title_recommendation r " +
            "LEFT JOIN tu_title_library t ON r.title_library_id = t.id AND t.is_deleted = 0 " +
            "WHERE r.user_id = #{userId} AND r.recommend_date = #{date} " +
            "ORDER BY r.created_at DESC")
    List<Map<String, Object>> findByUserAndDate(@Param("userId") String userId, @Param("date") LocalDate date);

    @Select("SELECT DISTINCT title_library_id FROM tu_title_recommendation WHERE recommend_date = #{date}")
    List<String> findMatchedTitleIdsByDate(@Param("date") LocalDate date);

    @Select("SELECT DISTINCT user_id FROM tu_title_recommendation WHERE recommend_date = #{date}")
    List<String> findMatchedUserIdsByDate(@Param("date") LocalDate date);

    @Delete("DELETE FROM tu_title_recommendation WHERE recommend_date = #{date}")
    int deleteByDate(@Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM tu_title_recommendation WHERE title_library_id = #{titleLibraryId}")
    int countByTitleId(@Param("titleLibraryId") String titleLibraryId);

    /** 查询指定日期下所有已存在的 user_id + track_id 组合 */
    @Select("SELECT DISTINCT user_id, track_id FROM tu_title_recommendation WHERE recommend_date = #{date}")
    List<Map<String, Object>> findUserTrackCombosByDate(@Param("date") LocalDate date);

    /** 查询指定用户历史上绑定过的所有标题ID */
    @Select("SELECT DISTINCT title_library_id FROM tu_title_recommendation WHERE user_id = #{userId}")
    List<String> findHistoricallyMatchedTitleIdsByUserId(@Param("userId") String userId);

    /** 查询指定用户历史上绑定的所有标题记录，按日期倒序 */
    @Select("SELECT r.recommend_date as recommendDate, t.title as titleName " +
            "FROM tu_title_recommendation r " +
            "INNER JOIN tu_title_library t ON r.title_library_id = t.id AND t.is_deleted = 0 " +
            "WHERE r.user_id = #{userId} " +
            "ORDER BY r.recommend_date DESC, r.created_at DESC")
    List<Map<String, Object>> findHistoryByUserId(@Param("userId") String userId);

    /** 查询指定用户指定日期已匹配的标题列表 */
    @Select("SELECT title_library_id as titleId FROM tu_title_recommendation WHERE user_id = #{userId} AND recommend_date = #{date}")
    List<Map<String, Object>> findMatchedTitlesByDateAndUser(@Param("date") LocalDate date, @Param("userId") String userId);

    /** 查询所有已经被绑定过用户的标题ID（全局） */
    @Select("SELECT DISTINCT title_library_id FROM tu_title_recommendation")
    List<String> findAllMatchedTitleIds();
}
