package com.example.blogger.mapper;

import com.example.blogger.entity.TitleLibrary;
import org.apache.ibatis.annotations.*;
import java.util.List;

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
            "ORDER BY t.created_at DESC" +
            "</script>")
    List<TitleLibrary> search(@Param("platform") String platform, @Param("trackId") String trackId, @Param("keyword") String keyword, @Param("recommendUserName") String recommendUserName, @Param("matched") String matched, @Param("pushDate") String pushDate);

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
            "ORDER BY t.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<TitleLibrary> searchPage(@Param("platform") String platform, @Param("trackId") String trackId, @Param("keyword") String keyword, @Param("recommendUserName") String recommendUserName, @Param("matched") String matched, @Param("pushDate") String pushDate, @Param("offset") int offset, @Param("limit") int limit);

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
            "</script>")
    int countSearch(@Param("platform") String platform, @Param("trackId") String trackId, @Param("keyword") String keyword, @Param("recommendUserName") String recommendUserName, @Param("matched") String matched, @Param("pushDate") String pushDate);

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
            "WHERE t.id = #{id} AND t.is_deleted = 0")
    TitleLibrary findById(String id);

    @Insert("INSERT INTO tu_title_library(id, title, description, push_date, platform, track_id, use_count, is_deleted, created_at) " +
            "VALUES(#{id}, #{title}, #{description}, #{pushDate}, #{platform}, #{trackId}, #{useCount}, 0, NOW())")
    int insert(TitleLibrary titleLibrary);

    @Update("UPDATE tu_title_library SET title=#{title}, description=#{description}, push_date=#{pushDate}, platform=#{platform}, track_id=#{trackId} WHERE id=#{id}")
    int update(TitleLibrary titleLibrary);

    @Update("UPDATE tu_title_library SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);

    @Select("SELECT COUNT(*) FROM tu_title_library WHERE is_deleted = 0")
    int countAll();
}
