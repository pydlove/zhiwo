package com.example.blogger.mapper;

import com.example.blogger.entity.TitleLibrary;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface TitleLibraryMapper {

    @Select("SELECT t.*, tr.name as trackName, " +
            "r.user_id as recommendUserId, u.username as recommendUserName, u.template as recommendUserTemplate, r.recommend_date as recommendDate, r.subscription_post_id as subscriptionPostId, " +
            "sp.title as subscriptionPostTitle, sp.file_url as subscriptionPostFileUrl " +
            "FROM tu_title_library t " +
            "LEFT JOIN tu_track tr ON t.track_id = tr.id AND tr.is_deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT r1.title_library_id, r1.user_id, r1.recommend_date, r1.subscription_post_id " +
            "  FROM tu_title_recommendation r1 " +
            "  INNER JOIN (" +
            "    SELECT title_library_id, MAX(created_at) as max_created_at " +
            "    FROM tu_title_recommendation " +
            "    GROUP BY title_library_id" +
            "  ) r2 ON r1.title_library_id = r2.title_library_id AND r1.created_at = r2.max_created_at" +
            ") r ON t.id = r.title_library_id " +
            "LEFT JOIN tu_user u ON r.user_id = u.id AND u.is_deleted = 0 " +
            "LEFT JOIN tu_subscription_post sp ON r.subscription_post_id = sp.id AND sp.is_deleted = 0 " +
            "WHERE t.is_deleted = 0 " +
            "ORDER BY t.created_at DESC")
    List<TitleLibrary> findAll();

    @Select("SELECT t.*, tr.name as trackName, " +
            "r.user_id as recommendUserId, u.username as recommendUserName, u.template as recommendUserTemplate, r.recommend_date as recommendDate, r.subscription_post_id as subscriptionPostId, " +
            "sp.title as subscriptionPostTitle, sp.file_url as subscriptionPostFileUrl " +
            "FROM tu_title_library t " +
            "LEFT JOIN tu_track tr ON t.track_id = tr.id AND tr.is_deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT r1.title_library_id, r1.user_id, r1.recommend_date, r1.subscription_post_id " +
            "  FROM tu_title_recommendation r1 " +
            "  INNER JOIN (" +
            "    SELECT title_library_id, MAX(created_at) as max_created_at " +
            "    FROM tu_title_recommendation " +
            "    GROUP BY title_library_id" +
            "  ) r2 ON r1.title_library_id = r2.title_library_id AND r1.created_at = r2.max_created_at" +
            ") r ON t.id = r.title_library_id " +
            "LEFT JOIN tu_user u ON r.user_id = u.id AND u.is_deleted = 0 " +
            "LEFT JOIN tu_subscription_post sp ON r.subscription_post_id = sp.id AND sp.is_deleted = 0 " +
            "WHERE t.is_deleted = 0 " +
            "ORDER BY t.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<TitleLibrary> findAllPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>SELECT t.*, tr.name as trackName, " +
            "r.user_id as recommendUserId, u.username as recommendUserName, u.template as recommendUserTemplate, r.recommend_date as recommendDate, r.subscription_post_id as subscriptionPostId, " +
            "sp.title as subscriptionPostTitle, sp.file_url as subscriptionPostFileUrl " +
            "FROM tu_title_library t " +
            "LEFT JOIN tu_track tr ON t.track_id = tr.id AND tr.is_deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT r1.title_library_id, r1.user_id, r1.recommend_date, r1.subscription_post_id " +
            "  FROM tu_title_recommendation r1 " +
            "  INNER JOIN (" +
            "    SELECT title_library_id, MAX(created_at) as max_created_at " +
            "    FROM tu_title_recommendation " +
            "    GROUP BY title_library_id" +
            "  ) r2 ON r1.title_library_id = r2.title_library_id AND r1.created_at = r2.max_created_at" +
            ") r ON t.id = r.title_library_id " +
            "LEFT JOIN tu_user u ON r.user_id = u.id AND u.is_deleted = 0 " +
            "LEFT JOIN tu_subscription_post sp ON r.subscription_post_id = sp.id AND sp.is_deleted = 0 " +
            "WHERE t.is_deleted = 0 " +
            "AND (#{platform} IS NULL OR #{platform} = '' OR t.platform = #{platform}) " +
            "AND (#{trackId} IS NULL OR #{trackId} = '' OR t.track_id = #{trackId}) " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' OR t.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{recommendUserName} IS NULL OR #{recommendUserName} = '' OR u.username LIKE CONCAT('%', #{recommendUserName}, '%')) " +
            "AND (#{matched} IS NULL OR #{matched} = '' OR (#{matched} = '1' AND r.user_id IS NOT NULL) OR (#{matched} = '0' AND r.user_id IS NULL)) " +
            "AND (#{pushDate} IS NULL OR #{pushDate} = '' OR t.push_date = #{pushDate}) " +
            "AND (#{isUsed} IS NULL OR #{isUsed} = '' OR t.is_used = #{isUsed}) " +
            "<if test='userType != null and userType != \"\"'> " +
            "AND (" +
            "  (#{userType} = 'accountOpened' AND u.is_account_opened = 1) OR " +
            "  (#{userType} = 'distributor' AND u.is_distributor = 1) OR " +
            "  (#{userType} = 'trial' AND u.is_trial = 1)" +
            ")" +
            "</if>" +
            "ORDER BY t.created_at DESC" +
            "</script>")
    List<TitleLibrary> search(@Param("platform") String platform, @Param("trackId") String trackId, @Param("keyword") String keyword, @Param("recommendUserName") String recommendUserName, @Param("matched") String matched, @Param("pushDate") String pushDate, @Param("isUsed") String isUsed, @Param("userType") String userType);

    @Select("<script>SELECT t.*, tr.name as trackName, " +
            "r.user_id as recommendUserId, u.username as recommendUserName, u.template as recommendUserTemplate, r.recommend_date as recommendDate, r.subscription_post_id as subscriptionPostId, " +
            "sp.title as subscriptionPostTitle, sp.file_url as subscriptionPostFileUrl " +
            "FROM tu_title_library t " +
            "LEFT JOIN tu_track tr ON t.track_id = tr.id AND tr.is_deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT r1.title_library_id, r1.user_id, r1.recommend_date, r1.subscription_post_id " +
            "  FROM tu_title_recommendation r1 " +
            "  INNER JOIN (" +
            "    SELECT title_library_id, MAX(created_at) as max_created_at " +
            "    FROM tu_title_recommendation " +
            "    GROUP BY title_library_id" +
            "  ) r2 ON r1.title_library_id = r2.title_library_id AND r1.created_at = r2.max_created_at" +
            ") r ON t.id = r.title_library_id " +
            "LEFT JOIN tu_user u ON r.user_id = u.id AND u.is_deleted = 0 " +
            "LEFT JOIN tu_subscription_post sp ON r.subscription_post_id = sp.id AND sp.is_deleted = 0 " +
            "WHERE t.is_deleted = 0 " +
            "AND (#{platform} IS NULL OR #{platform} = '' OR t.platform = #{platform}) " +
            "AND (#{trackId} IS NULL OR #{trackId} = '' OR t.track_id = #{trackId}) " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' OR t.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{recommendUserName} IS NULL OR #{recommendUserName} = '' OR u.username LIKE CONCAT('%', #{recommendUserName}, '%')) " +
            "AND (#{matched} IS NULL OR #{matched} = '' OR (#{matched} = '1' AND r.user_id IS NOT NULL) OR (#{matched} = '0' AND r.user_id IS NULL)) " +
            "AND (#{pushDate} IS NULL OR #{pushDate} = '' OR t.push_date = #{pushDate}) " +
            "AND (#{isUsed} IS NULL OR #{isUsed} = '' OR t.is_used = #{isUsed}) " +
            "<if test='userType != null and userType != \"\"'> " +
            "AND (" +
            "  (#{userType} = 'accountOpened' AND u.is_account_opened = 1) OR " +
            "  (#{userType} = 'distributor' AND u.is_distributor = 1) OR " +
            "  (#{userType} = 'trial' AND u.is_trial = 1)" +
            ")" +
            "</if>" +
            "ORDER BY t.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<TitleLibrary> searchPage(@Param("platform") String platform, @Param("trackId") String trackId, @Param("keyword") String keyword, @Param("recommendUserName") String recommendUserName, @Param("matched") String matched, @Param("pushDate") String pushDate, @Param("isUsed") String isUsed, @Param("userType") String userType, @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>SELECT COUNT(*) " +
            "FROM tu_title_library t " +
            "LEFT JOIN tu_track tr ON t.track_id = tr.id AND tr.is_deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT r1.title_library_id, r1.user_id, r1.recommend_date, r1.subscription_post_id " +
            "  FROM tu_title_recommendation r1 " +
            "  INNER JOIN (" +
            "    SELECT title_library_id, MAX(created_at) as max_created_at " +
            "    FROM tu_title_recommendation " +
            "    GROUP BY title_library_id" +
            "  ) r2 ON r1.title_library_id = r2.title_library_id AND r1.created_at = r2.max_created_at" +
            ") r ON t.id = r.title_library_id " +
            "LEFT JOIN tu_user u ON r.user_id = u.id AND u.is_deleted = 0 " +
            "WHERE t.is_deleted = 0 " +
            "AND (#{platform} IS NULL OR #{platform} = '' OR t.platform = #{platform}) " +
            "AND (#{trackId} IS NULL OR #{trackId} = '' OR t.track_id = #{trackId}) " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' OR t.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{recommendUserName} IS NULL OR #{recommendUserName} = '' OR u.username LIKE CONCAT('%', #{recommendUserName}, '%')) " +
            "AND (#{matched} IS NULL OR #{matched} = '' OR (#{matched} = '1' AND r.user_id IS NOT NULL) OR (#{matched} = '0' AND r.user_id IS NULL)) " +
            "AND (#{pushDate} IS NULL OR #{pushDate} = '' OR t.push_date = #{pushDate}) " +
            "AND (#{isUsed} IS NULL OR #{isUsed} = '' OR t.is_used = #{isUsed}) " +
            "<if test='userType != null and userType != \"\"'> " +
            "AND (" +
            "  (#{userType} = 'accountOpened' AND u.is_account_opened = 1) OR " +
            "  (#{userType} = 'distributor' AND u.is_distributor = 1) OR " +
            "  (#{userType} = 'trial' AND u.is_trial = 1)" +
            ")" +
            "</if>" +
            "</script>")
    int countSearch(@Param("platform") String platform, @Param("trackId") String trackId, @Param("keyword") String keyword, @Param("recommendUserName") String recommendUserName, @Param("matched") String matched, @Param("pushDate") String pushDate, @Param("isUsed") String isUsed, @Param("userType") String userType);

    @Select("SELECT t.*, t.track_id as trackId, tr.name as trackName, " +
            "r.user_id as recommendUserId, u.username as recommendUserName, u.template as recommendUserTemplate, r.recommend_date as recommendDate, r.subscription_post_id as subscriptionPostId, " +
            "sp.title as subscriptionPostTitle, sp.file_url as subscriptionPostFileUrl " +
            "FROM tu_title_library t " +
            "LEFT JOIN tu_track tr ON t.track_id = tr.id AND tr.is_deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT r1.title_library_id, r1.user_id, r1.recommend_date, r1.subscription_post_id " +
            "  FROM tu_title_recommendation r1 " +
            "  INNER JOIN (" +
            "    SELECT title_library_id, MAX(created_at) as max_created_at " +
            "    FROM tu_title_recommendation " +
            "    GROUP BY title_library_id" +
            "  ) r2 ON r1.title_library_id = r2.title_library_id AND r1.created_at = r2.max_created_at" +
            ") r ON t.id = r.title_library_id " +
            "LEFT JOIN tu_user u ON r.user_id = u.id AND u.is_deleted = 0 " +
            "LEFT JOIN tu_subscription_post sp ON r.subscription_post_id = sp.id AND sp.is_deleted = 0 " +
            "WHERE t.id = #{id} AND t.is_deleted = 0")
    TitleLibrary findById(String id);

    @Insert("INSERT INTO tu_title_library(id, title, description, push_date, platform, track_id, use_count, is_used, is_deleted, created_at) " +
            "VALUES(#{id}, #{title}, #{description}, #{pushDate}, #{platform}, #{trackId}, #{useCount}, #{isUsed}, 0, NOW())")
    int insert(TitleLibrary titleLibrary);

    @Update("UPDATE tu_title_library SET title=#{title}, description=#{description}, push_date=#{pushDate}, platform=#{platform}, track_id=#{trackId}, is_used=#{isUsed} WHERE id=#{id}")
    int update(TitleLibrary titleLibrary);

    @Update("UPDATE tu_title_library SET is_used=#{isUsed} WHERE id=#{id}")
    int updateIsUsed(@Param("id") String id, @Param("isUsed") Integer isUsed);

    @Update("UPDATE tu_title_library SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);

    @Select("SELECT COUNT(*) FROM tu_title_library WHERE is_deleted = 0")
    int countAll();

    @Select("SELECT t.* FROM tu_title_library t WHERE t.is_deleted = 0 AND t.push_date = #{pushDate}")
    List<TitleLibrary> findByPushDate(@Param("pushDate") String pushDate);

    @Select("SELECT t.*, t.track_id as trackId FROM tu_title_library t WHERE t.is_deleted = 0 AND t.title = #{title} AND (t.platform = #{platform} OR (#{platform} IS NULL AND t.platform IS NULL)) LIMIT 1")
    TitleLibrary findByTitlePlatformTrack(@Param("title") String title, @Param("platform") String platform, @Param("trackId") String trackId);

    @Select("SELECT " +
            "u.id as userId, u.username, u.email, " +
            "u.is_account_opened as isAccountOpened, u.is_distributor as isDistributor, u.is_trial as isTrial, " +
            "t.id as trackId, t.name as trackName, " +
            "r.id as recommendationId, r.subscription_post_id as subscriptionPostId, r.title_library_id as titleLibraryId, " +
            "sp.title as postTitle, tl.title as titleName " +
            "FROM tu_user u " +
            "INNER JOIN tu_user_track ut ON u.id = ut.user_id " +
            "INNER JOIN tu_track t ON ut.track_id = t.id AND t.is_deleted = 0 " +
            "LEFT JOIN tu_title_recommendation r ON u.id = r.user_id " +
            "    AND r.recommend_date = #{date} " +
            "    AND r.track_id = ut.track_id " +
            "LEFT JOIN tu_subscription_post sp ON r.subscription_post_id = sp.id AND sp.is_deleted = 0 " +
            "LEFT JOIN tu_title_library tl ON r.title_library_id = tl.id AND tl.is_deleted = 0 " +
            "WHERE u.status = 1 AND u.is_deleted = 0 " +
            "    AND (u.is_account_opened = 1 OR u.is_distributor = 1 OR u.is_trial = 1) " +
            "ORDER BY u.id, ut.created_at")
    List<Map<String, Object>> findPushOverview(@Param("date") LocalDate date);
}
