package com.example.blogger.mapper;

import com.example.blogger.entity.TitleLibrary;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface TitleLibraryMapper {

    @Select("SELECT t.*, t.track_id as trackId, tr.name as trackName, " +
            "r.user_id as recommendUserId, u.username as recommendUserName, u.template as recommendUserTemplate, r.recommend_date as recommendDate, r.push_date as pushDate, " +
            "" +
            "t.generated_file_url as generatedFileUrl, t.generated_file_name as generatedFileName, t.generated_at as generatedAt, t.generate_status as generateStatus " +
            "FROM tu_title_library t " +
            "LEFT JOIN tu_track tr ON t.track_id = tr.id AND tr.is_deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT r1.title_library_id, MAX(r1.user_id) as user_id, MAX(r1.recommend_date) as recommend_date, MAX(r1.recommend_date) as push_date" +
            "  FROM tu_title_recommendation r1 " +
            "  INNER JOIN (" +
            "    SELECT title_library_id, MAX(created_at) as max_created_at " +
            "    FROM tu_title_recommendation " +
            "    GROUP BY title_library_id" +
            "  ) r2 ON r1.title_library_id = r2.title_library_id AND r1.created_at = r2.max_created_at" +
            "  GROUP BY r1.title_library_id" +
            ") r ON t.id = r.title_library_id " +
            "LEFT JOIN tu_user u ON r.user_id = u.id AND u.is_deleted = 0 " +
            "" +
            "WHERE t.is_deleted = 0 " +
            "AND (t.track_id IS NULL OR tr.id IS NOT NULL) " +
            "ORDER BY t.created_at DESC")
    List<TitleLibrary> findAll();

    @Select("SELECT t.*, t.track_id as trackId, tr.name as trackName, " +
            "r.user_id as recommendUserId, u.username as recommendUserName, u.template as recommendUserTemplate, r.recommend_date as recommendDate, r.push_date as pushDate, " +
            "" +
            "t.generated_file_url as generatedFileUrl, t.generated_file_name as generatedFileName, t.generated_at as generatedAt, t.generate_status as generateStatus " +
            "FROM tu_title_library t " +
            "LEFT JOIN tu_track tr ON t.track_id = tr.id AND tr.is_deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT r1.title_library_id, MAX(r1.user_id) as user_id, MAX(r1.recommend_date) as recommend_date, MAX(r1.recommend_date) as push_date" +
            "  FROM tu_title_recommendation r1 " +
            "  INNER JOIN (" +
            "    SELECT title_library_id, MAX(created_at) as max_created_at " +
            "    FROM tu_title_recommendation " +
            "    GROUP BY title_library_id" +
            "  ) r2 ON r1.title_library_id = r2.title_library_id AND r1.created_at = r2.max_created_at" +
            "  GROUP BY r1.title_library_id" +
            ") r ON t.id = r.title_library_id " +
            "LEFT JOIN tu_user u ON r.user_id = u.id AND u.is_deleted = 0 " +
            "" +
            "WHERE t.is_deleted = 0 " +
            "AND (t.track_id IS NULL OR tr.id IS NOT NULL) " +
            "ORDER BY t.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<TitleLibrary> findAllPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>SELECT t.*, t.track_id as trackId, tr.name as trackName, " +
            "r.user_id as recommendUserId, u.username as recommendUserName, u.template as recommendUserTemplate, r.recommend_date as recommendDate, r.push_date as pushDate, " +
            "" +
            "t.generated_file_url as generatedFileUrl, t.generated_file_name as generatedFileName, t.generated_at as generatedAt, t.generate_status as generateStatus " +
            "FROM tu_title_library t " +
            "LEFT JOIN tu_track tr ON t.track_id = tr.id AND tr.is_deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT r1.title_library_id, MAX(r1.user_id) as user_id, MAX(r1.recommend_date) as recommend_date, MAX(r1.recommend_date) as push_date" +
            "  FROM tu_title_recommendation r1 " +
            "  INNER JOIN (" +
            "    SELECT title_library_id, MAX(created_at) as max_created_at " +
            "    FROM tu_title_recommendation " +
            "    GROUP BY title_library_id" +
            "  ) r2 ON r1.title_library_id = r2.title_library_id AND r1.created_at = r2.max_created_at" +
            "  GROUP BY r1.title_library_id" +
            ") r ON t.id = r.title_library_id " +
            "LEFT JOIN tu_user u ON r.user_id = u.id AND u.is_deleted = 0 " +
            "" +
            "WHERE t.is_deleted = 0 " +
            "AND (t.track_id IS NULL OR tr.id IS NOT NULL) " +
            "AND (#{platform} IS NULL OR #{platform} = '' OR t.platform LIKE CONCAT('%', #{platform}, '%')) " +
            "AND (#{trackId} IS NULL OR #{trackId} = '' OR t.track_id = #{trackId}) " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' OR t.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{recommendUserName} IS NULL OR #{recommendUserName} = '' OR u.username LIKE CONCAT('%', #{recommendUserName}, '%')) " +
            "AND (#{matched} IS NULL OR #{matched} = '' OR (#{matched} = '1' AND r.user_id IS NOT NULL) OR (#{matched} = '0' AND r.user_id IS NULL)) " +
            "AND (#{pushDate} IS NULL OR #{pushDate} = '' OR r.recommend_date = #{pushDate}) " +
            "AND (#{isUsed} IS NULL OR #{isUsed} = '' OR t.is_used = #{isUsed}) " +
            "AND (#{isConfirmed} IS NULL OR #{isConfirmed} = '' OR t.confirm_status = #{isConfirmed})" +
            "<if test='aiFlavor != null and aiFlavor != &quot;&quot;'> " +
            "  <choose>" +
            "    <when test='aiFlavor == &quot;0&quot;'>AND (t.ai_flavor_status IS NULL OR t.ai_flavor_status = 0)</when>" +
            "    <when test='aiFlavor == &quot;1&quot;'>AND t.ai_flavor_status = 1</when>" +
            "    <when test='aiFlavor == &quot;2&quot;'>AND t.ai_flavor_status = 2</when>" +
            "  </choose>" +
            "</if>" +
            "<if test='matchable != null and matchable != \"\"'> " +
            "AND r.user_id IS NULL AND (t.is_used IS NULL OR t.is_used != 1) AND (t.track_id IS NOT NULL AND t.track_id != '') " +
            "</if>" +
            "<if test='userType != null and userType != \"\"'> " +
            "AND (" +
            "  r.user_id IS NULL OR " +
            "  (#{userType} = '1' AND u.user_type = 1) OR " +
            "  (#{userType} = '2' AND u.user_type = 2) OR " +
            "  (#{userType} = '3' AND u.user_type = 3)" +
            ")" +
            "</if>" +
            "<choose>" +
            "  <when test=\"sortField != null and sortField != ''\">" +
            "    <choose>" +
            "      <when test=\"sortField == 'recommendUserName'\">ORDER BY u.username</when>" +
            "      <when test=\"sortField == 'pushDate'\">ORDER BY r.recommend_date</when>" +
            "      <when test=\"sortField == 'createdAt'\">ORDER BY t.created_at</when>" +
            "      <otherwise>ORDER BY t.created_at</otherwise>" +
            "    </choose>" +
            "    <choose>" +
            "      <when test=\"sortOrder == 'ascend'\"> ASC</when>" +
            "      <otherwise> DESC</otherwise>" +
            "    </choose>" +
            "  </when>" +
            "  <otherwise>ORDER BY t.created_at DESC</otherwise>" +
            "</choose>" +
            "</script>")
    List<TitleLibrary> search(@Param("platform") String platform, @Param("trackId") String trackId, @Param("keyword") String keyword, @Param("recommendUserName") String recommendUserName, @Param("matched") String matched, @Param("pushDate") String pushDate, @Param("isUsed") String isUsed, @Param("isConfirmed") String isConfirmed, @Param("aiFlavor") String aiFlavor, @Param("userType") String userType, @Param("matchable") String matchable, @Param("sortField") String sortField, @Param("sortOrder") String sortOrder);

    @Select("<script>SELECT t.*, t.track_id as trackId, tr.name as trackName, " +
            "r.user_id as recommendUserId, u.username as recommendUserName, u.template as recommendUserTemplate, r.recommend_date as recommendDate, r.push_date as pushDate, " +
            "" +
            "t.generated_file_url as generatedFileUrl, t.generated_file_name as generatedFileName, t.generated_at as generatedAt, t.generate_status as generateStatus " +
            "FROM tu_title_library t " +
            "LEFT JOIN tu_track tr ON t.track_id = tr.id AND tr.is_deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT r1.title_library_id, MAX(r1.user_id) as user_id, MAX(r1.recommend_date) as recommend_date, MAX(r1.recommend_date) as push_date" +
            "  FROM tu_title_recommendation r1 " +
            "  INNER JOIN (" +
            "    SELECT title_library_id, MAX(created_at) as max_created_at " +
            "    FROM tu_title_recommendation " +
            "    GROUP BY title_library_id" +
            "  ) r2 ON r1.title_library_id = r2.title_library_id AND r1.created_at = r2.max_created_at" +
            "  GROUP BY r1.title_library_id" +
            ") r ON t.id = r.title_library_id " +
            "LEFT JOIN tu_user u ON r.user_id = u.id AND u.is_deleted = 0 " +
            "" +
            "WHERE t.is_deleted = 0 " +
            "AND (t.track_id IS NULL OR tr.id IS NOT NULL) " +
            "AND (#{platform} IS NULL OR #{platform} = '' OR t.platform LIKE CONCAT('%', #{platform}, '%')) " +
            "AND (#{trackId} IS NULL OR #{trackId} = '' OR t.track_id = #{trackId}) " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' OR t.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{recommendUserName} IS NULL OR #{recommendUserName} = '' OR u.username LIKE CONCAT('%', #{recommendUserName}, '%')) " +
            "AND (#{matched} IS NULL OR #{matched} = '' OR (#{matched} = '1' AND r.user_id IS NOT NULL) OR (#{matched} = '0' AND r.user_id IS NULL)) " +
            "AND (#{pushDate} IS NULL OR #{pushDate} = '' OR r.recommend_date = #{pushDate}) " +
            "AND (#{isUsed} IS NULL OR #{isUsed} = '' OR t.is_used = #{isUsed}) " +
            "AND (#{isConfirmed} IS NULL OR #{isConfirmed} = '' OR t.confirm_status = #{isConfirmed})" +
            "<if test='aiFlavor != null and aiFlavor != &quot;&quot;'> " +
            "  <choose>" +
            "    <when test='aiFlavor == &quot;0&quot;'>AND (t.ai_flavor_status IS NULL OR t.ai_flavor_status = 0)</when>" +
            "    <when test='aiFlavor == &quot;1&quot;'>AND t.ai_flavor_status = 1</when>" +
            "    <when test='aiFlavor == &quot;2&quot;'>AND t.ai_flavor_status = 2</when>" +
            "  </choose>" +
            "</if>" +
            "<if test='matchable != null and matchable != \"\"'> " +
            "AND r.user_id IS NULL AND (t.is_used IS NULL OR t.is_used != 1) AND (t.track_id IS NOT NULL AND t.track_id != '') " +
            "</if>" +
            "<if test='userType != null and userType != \"\"'> " +
            "AND (" +
            "  r.user_id IS NULL OR " +
            "  (#{userType} = '1' AND u.user_type = 1) OR " +
            "  (#{userType} = '2' AND u.user_type = 2) OR " +
            "  (#{userType} = '3' AND u.user_type = 3)" +
            ")" +
            "</if>" +
            "<choose>" +
            "  <when test=\"sortField != null and sortField != ''\">" +
            "    <choose>" +
            "      <when test=\"sortField == 'recommendUserName'\">ORDER BY u.username</when>" +
            "      <when test=\"sortField == 'pushDate'\">ORDER BY r.recommend_date</when>" +
            "      <when test=\"sortField == 'createdAt'\">ORDER BY t.created_at</when>" +
            "      <otherwise>ORDER BY t.created_at</otherwise>" +
            "    </choose>" +
            "    <choose>" +
            "      <when test=\"sortOrder == 'ascend'\"> ASC</when>" +
            "      <otherwise> DESC</otherwise>" +
            "    </choose>" +
            "  </when>" +
            "  <otherwise>ORDER BY t.created_at DESC</otherwise>" +
            "</choose> " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<TitleLibrary> searchPage(@Param("platform") String platform, @Param("trackId") String trackId, @Param("keyword") String keyword, @Param("recommendUserName") String recommendUserName, @Param("matched") String matched, @Param("pushDate") String pushDate, @Param("isUsed") String isUsed, @Param("isConfirmed") String isConfirmed, @Param("aiFlavor") String aiFlavor, @Param("userType") String userType, @Param("matchable") String matchable, @Param("offset") int offset, @Param("limit") int limit, @Param("sortField") String sortField, @Param("sortOrder") String sortOrder);

    @Select("<script>SELECT COUNT(DISTINCT t.id) " +
            "FROM tu_title_library t " +
            "LEFT JOIN tu_track tr ON t.track_id = tr.id AND tr.is_deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT r1.title_library_id, MAX(r1.user_id) as user_id, MAX(r1.recommend_date) as recommend_date " +
            "  FROM tu_title_recommendation r1 " +
            "  INNER JOIN (" +
            "    SELECT title_library_id, MAX(created_at) as max_created_at " +
            "    FROM tu_title_recommendation " +
            "    GROUP BY title_library_id" +
            "  ) r2 ON r1.title_library_id = r2.title_library_id AND r1.created_at = r2.max_created_at" +
            "  GROUP BY r1.title_library_id" +
            ") r ON t.id = r.title_library_id " +
            "LEFT JOIN tu_user u ON r.user_id = u.id AND u.is_deleted = 0 " +
            "WHERE t.is_deleted = 0 " +
            "AND (t.track_id IS NULL OR tr.id IS NOT NULL) " +
            "AND (#{platform} IS NULL OR #{platform} = '' OR t.platform LIKE CONCAT('%', #{platform}, '%')) " +
            "AND (#{trackId} IS NULL OR #{trackId} = '' OR t.track_id = #{trackId}) " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' OR t.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{recommendUserName} IS NULL OR #{recommendUserName} = '' OR u.username LIKE CONCAT('%', #{recommendUserName}, '%')) " +
            "AND (#{matched} IS NULL OR #{matched} = '' OR (#{matched} = '1' AND r.user_id IS NOT NULL) OR (#{matched} = '0' AND r.user_id IS NULL)) " +
            "AND (#{pushDate} IS NULL OR #{pushDate} = '' OR r.recommend_date = #{pushDate}) " +
            "AND (#{isUsed} IS NULL OR #{isUsed} = '' OR t.is_used = #{isUsed}) " +
            "AND (#{isConfirmed} IS NULL OR #{isConfirmed} = '' OR t.confirm_status = #{isConfirmed})" +
            "<if test='aiFlavor != null and aiFlavor != &quot;&quot;'> " +
            "  <choose>" +
            "    <when test='aiFlavor == &quot;0&quot;'>AND (t.ai_flavor_status IS NULL OR t.ai_flavor_status = 0)</when>" +
            "    <when test='aiFlavor == &quot;1&quot;'>AND t.ai_flavor_status = 1</when>" +
            "    <when test='aiFlavor == &quot;2&quot;'>AND t.ai_flavor_status = 2</when>" +
            "  </choose>" +
            "</if>" +
            "<if test='matchable != null and matchable != \"\"'> " +
            "AND r.user_id IS NULL AND (t.is_used IS NULL OR t.is_used != 1) AND (t.track_id IS NOT NULL AND t.track_id != '') " +
            "</if>" +
            "<if test='userType != null and userType != \"\"'> " +
            "AND (" +
            "  r.user_id IS NULL OR " +
            "  (#{userType} = '1' AND u.user_type = 1) OR " +
            "  (#{userType} = '2' AND u.user_type = 2) OR " +
            "  (#{userType} = '3' AND u.user_type = 3)" +
            ")" +
            "</if>" +
            "</script>")
    int countSearch(@Param("platform") String platform, @Param("trackId") String trackId, @Param("keyword") String keyword, @Param("recommendUserName") String recommendUserName, @Param("matched") String matched, @Param("pushDate") String pushDate, @Param("isUsed") String isUsed, @Param("isConfirmed") String isConfirmed, @Param("aiFlavor") String aiFlavor, @Param("userType") String userType, @Param("matchable") String matchable);

    @Select("SELECT t.*, t.track_id as trackId, tr.name as trackName, " +
            "r.user_id as recommendUserId, u.username as recommendUserName, u.template as recommendUserTemplate, r.recommend_date as recommendDate, r.push_date as pushDate, " +
            "" +
            "t.generated_file_url as generatedFileUrl, t.generated_file_name as generatedFileName, t.generated_at as generatedAt, t.generate_status as generateStatus " +
            "FROM tu_title_library t " +
            "LEFT JOIN tu_track tr ON t.track_id = tr.id AND tr.is_deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT r1.title_library_id, MAX(r1.user_id) as user_id, MAX(r1.recommend_date) as recommend_date, MAX(r1.recommend_date) as push_date" +
            "  FROM tu_title_recommendation r1 " +
            "  INNER JOIN (" +
            "    SELECT title_library_id, MAX(created_at) as max_created_at " +
            "    FROM tu_title_recommendation " +
            "    GROUP BY title_library_id" +
            "  ) r2 ON r1.title_library_id = r2.title_library_id AND r1.created_at = r2.max_created_at" +
            "  GROUP BY r1.title_library_id" +
            ") r ON t.id = r.title_library_id " +
            "LEFT JOIN tu_user u ON r.user_id = u.id AND u.is_deleted = 0 " +
            "" +
            "WHERE t.id = #{id} AND t.is_deleted = 0 ")
    TitleLibrary findById(String id);

    @Insert("INSERT INTO tu_title_library(id, title, description, platform, track_id, use_count, is_used, is_deleted, created_at, task_id, title_keyword) " +
            "VALUES(#{id}, #{title}, #{description}, #{platform}, #{trackId}, #{useCount}, #{isUsed}, 0, NOW(), #{taskId}, #{titleKeyword})")
    int insert(TitleLibrary titleLibrary);

    @Update("<script>UPDATE tu_title_library <set>" +
            "<if test='title != null'>title=#{title},</if>" +
            "<if test='description != null'>description=#{description},</if>" +
            "<if test='platform != null'>platform=#{platform},</if>" +
            "<if test='trackId != null'>track_id=#{trackId},</if>" +
            "<if test='taskId != null'>task_id=#{taskId},</if>" +
            "<if test='isUsed != null'>is_used=#{isUsed},</if>" +
            "</set> WHERE id=#{id}</script>")
    int update(TitleLibrary titleLibrary);

    @Update("UPDATE tu_title_library SET is_used=#{isUsed} WHERE id=#{id}")
    int updateIsUsed(@Param("id") String id, @Param("isUsed") Integer isUsed);

    @Update("UPDATE tu_title_library SET is_copied=#{isCopied} WHERE id=#{id}")
    int updateIsCopied(@Param("id") String id, @Param("isCopied") Integer isCopied);

    @Update("UPDATE tu_title_library SET is_confirmed=#{isConfirmed} WHERE id=#{id}")
    int updateIsConfirmed(@Param("id") String id, @Param("isConfirmed") Integer isConfirmed);

    @Update("UPDATE tu_title_library SET generated_file_url = #{generatedFileUrl}, generated_file_name = #{generatedFileName}, generated_at = #{generatedAt}, generate_status = 1 WHERE id = #{id}")
    int updateGeneratedFile(@Param("id") String id, @Param("generatedFileUrl") String generatedFileUrl, @Param("generatedFileName") String generatedFileName, @Param("generatedAt") LocalDateTime generatedAt);

    @Update("UPDATE tu_title_library SET generate_status = #{generateStatus} WHERE id = #{id}")
    int updateGenerateStatus(@Param("id") String id, @Param("generateStatus") Integer generateStatus);

    @Update("UPDATE tu_title_library SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);

    @Update("UPDATE tu_title_library SET ai_flavor_status = #{aiFlavorStatus} WHERE id = #{id}")
    int updateAiFlavorStatus(@Param("id") String id, @Param("aiFlavorStatus") Integer aiFlavorStatus);

    @Update("UPDATE tu_title_library SET confirm_status = #{confirmStatus} WHERE id = #{id}")
    int updateConfirmStatus(@Param("id") String id, @Param("confirmStatus") Integer confirmStatus);

    @Update("UPDATE tu_title_library SET banned_word_check_result = #{result} WHERE id = #{id}")
    int updateBannedWordCheckResult(@Param("id") String id, @Param("result") String result);

    @Update("UPDATE tu_title_library SET image_post_urls = #{imagePostUrls} WHERE id = #{id}")
    int updateImagePostUrls(@Param("id") String id, @Param("imagePostUrls") String imagePostUrls);

    /**
     * 查询待审核列表：已生成文章、未确认、指定推荐日期
     */
    @Select("SELECT t.*, t.track_id as trackId, tr.name as trackName, " +
            "r.user_id as recommendUserId, u.username as recommendUserName, u.template as recommendUserTemplate, r.recommend_date as recommendDate, " +
            "t.generated_file_url as generatedFileUrl, t.generated_file_name as generatedFileName, t.generated_at as generatedAt, t.generate_status as generateStatus, " +
            "t.confirm_status as confirmStatus, t.ai_flavor_status as aiFlavorStatus " +
            "FROM tu_title_library t " +
            "LEFT JOIN tu_track tr ON t.track_id = tr.id AND tr.is_deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT r1.title_library_id, MAX(r1.user_id) as user_id, MAX(r1.recommend_date) as recommend_date " +
            "  FROM tu_title_recommendation r1 " +
            "  INNER JOIN (" +
            "    SELECT title_library_id, MAX(created_at) as max_created_at " +
            "    FROM tu_title_recommendation " +
            "    GROUP BY title_library_id" +
            "  ) r2 ON r1.title_library_id = r2.title_library_id AND r1.created_at = r2.max_created_at" +
            "  GROUP BY r1.title_library_id" +
            ") r ON t.id = r.title_library_id " +
            "LEFT JOIN tu_user u ON r.user_id = u.id AND u.is_deleted = 0 " +
            "WHERE t.is_deleted = 0 " +
            "AND t.generate_status = 1 " +
            "AND (t.confirm_status IS NULL OR t.confirm_status = 0) " +
            "AND r.recommend_date = #{recommendDate} " +
            "ORDER BY t.created_at DESC")
    List<TitleLibrary> findPendingReview(@Param("recommendDate") String recommendDate);

    /**
     * 查询审核历史：已确认或已拒绝、指定推荐日期
     */
    @Select("SELECT t.*, t.track_id as trackId, tr.name as trackName, " +
            "r.user_id as recommendUserId, u.username as recommendUserName, u.template as recommendUserTemplate, r.recommend_date as recommendDate, " +
            "t.generated_file_url as generatedFileUrl, t.generated_file_name as generatedFileName, t.generated_at as generatedAt, t.generate_status as generateStatus, " +
            "t.confirm_status as confirmStatus, t.ai_flavor_status as aiFlavorStatus " +
            "FROM tu_title_library t " +
            "LEFT JOIN tu_track tr ON t.track_id = tr.id AND tr.is_deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT r1.title_library_id, MAX(r1.user_id) as user_id, MAX(r1.recommend_date) as recommend_date " +
            "  FROM tu_title_recommendation r1 " +
            "  INNER JOIN (" +
            "    SELECT title_library_id, MAX(created_at) as max_created_at " +
            "    FROM tu_title_recommendation " +
            "    GROUP BY title_library_id" +
            "  ) r2 ON r1.title_library_id = r2.title_library_id AND r1.created_at = r2.max_created_at" +
            "  GROUP BY r1.title_library_id" +
            ") r ON t.id = r.title_library_id " +
            "LEFT JOIN tu_user u ON r.user_id = u.id AND u.is_deleted = 0 " +
            "WHERE t.is_deleted = 0 " +
            "AND t.confirm_status IN (1, 2) " +
            "AND r.recommend_date = #{recommendDate} " +
            "ORDER BY t.updated_at DESC")
    List<TitleLibrary> findReviewHistory(@Param("recommendDate") String recommendDate);

    @Select("SELECT COUNT(*) FROM tu_title_library WHERE is_deleted = 0")
    int countAll();

    @Select("SELECT t.* FROM tu_title_library t INNER JOIN tu_title_recommendation r ON t.id = r.title_library_id WHERE t.is_deleted = 0 AND r.recommend_date = #{pushDate}")
    List<TitleLibrary> findByPushDate(@Param("pushDate") String pushDate);

    /**
     * 统计指定推送日期下，各平台+赛道组合中已经有关联文章的标题数量
     * （通过 title_recommendation.recommend_date = pushDate 且 title_library.generated_file_url IS NOT NULL 判断）
     */
    @Select("SELECT t.platform, t.track_id as trackId, COUNT(DISTINCT r.title_library_id) as cnt " +
            "FROM tu_title_recommendation r " +
            "INNER JOIN tu_title_library t ON r.title_library_id = t.id AND t.is_deleted = 0 " +
            "WHERE r.recommend_date = #{pushDate} AND t.generated_file_url IS NOT NULL AND t.generated_file_url != '' " +
            "GROUP BY t.platform, t.track_id")
    List<Map<String, Object>> countCompletedByCombo(@Param("pushDate") String pushDate);

    @Select("SELECT t.*, t.track_id as trackId, t.title as title, t.description as description, t.platform as platform, t.created_at as createdAt, t.generate_status as generateStatus FROM tu_title_library t WHERE t.is_deleted = 0 AND t.generate_status = 1 ORDER BY t.created_at DESC")
    List<TitleLibrary> findAllGenerated();

    @Select("SELECT t.*, t.track_id as trackId FROM tu_title_library t WHERE t.is_deleted = 0 AND t.title = #{title} LIMIT 1")
    TitleLibrary findByTitle(@Param("title") String title);

    @Select("SELECT t.*, t.track_id as trackId FROM tu_title_library t WHERE t.is_deleted = 0 AND t.title = #{title} AND (t.platform = #{platform} OR (#{platform} IS NULL AND t.platform IS NULL)) LIMIT 1")
    TitleLibrary findByTitlePlatformTrack(@Param("title") String title, @Param("platform") String platform, @Param("trackId") String trackId);

    /**
     * 查询与给定关键词有交集的现有标题（用于相似度检测）
     */
    @Select("<script>" +
            "SELECT t.*, t.track_id as trackId FROM tu_title_library t WHERE t.is_deleted = 0 AND t.title_keyword IS NOT NULL AND t.title_keyword != '' " +
            "<if test='keywords != null and keywords.size > 0'>" +
            " AND (" +
            "<foreach collection='keywords' item='kw' separator=' OR '>" +
            " t.title_keyword LIKE CONCAT('%', #{kw}, '%') " +
            "</foreach>" +
            " )" +
            "</if>" +
            "</script>")
    List<TitleLibrary> findSimilarTitles(@Param("keywords") List<String> keywords);

    @Select("SELECT t.*, t.track_id as trackId, tr.name as trackName FROM tu_title_library t " +
            "LEFT JOIN tu_track tr ON t.track_id = tr.id AND tr.is_deleted = 0 " +
            "WHERE t.is_deleted = 0 AND t.task_id = #{taskId} " +
            "ORDER BY t.created_at DESC")
    List<TitleLibrary> findByTaskId(@Param("taskId") String taskId);

    @Select("<script>SELECT t.*, t.track_id as trackId FROM tu_title_library t WHERE t.is_deleted = 0 AND t.track_id IN " +
            "<foreach collection='trackIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "ORDER BY t.created_at DESC LIMIT #{limit}</script>")
    List<TitleLibrary> findRecentByTrackIds(@Param("trackIds") List<String> trackIds, @Param("limit") int limit);

    @Select("SELECT " +
            "u.id as userId, u.username, u.wx_name as wxName, u.email, " +
            "u.user_type as userType, " +
            "t.id as trackId, t.name as trackName, " +
            "r.id as recommendationId, r.title_library_id as titleLibraryId, " +
            "tl.title as titleName, tl.generated_file_url as generatedFileUrl " +
            "FROM tu_user u " +
            "INNER JOIN tu_user_track ut ON u.id = ut.user_id " +
            "INNER JOIN tu_track t ON ut.track_id = t.id AND t.is_deleted = 0 " +
            "LEFT JOIN tu_title_recommendation r ON u.id = r.user_id " +
            "    AND r.recommend_date = #{date} " +
            "    AND (r.track_id = ut.track_id OR r.track_id IS NULL OR r.track_id = '') " +
            "" +
            "LEFT JOIN tu_title_library tl ON r.title_library_id = tl.id AND tl.is_deleted = 0 " +
            "WHERE u.status = 1 AND u.is_deleted = 0 " +
            "    AND u.user_type IN (1, 2, 3) " +
            "ORDER BY u.id, ut.created_at")
    List<Map<String, Object>> findPushOverview(@Param("date") LocalDate date);

    /** 临时：把所有已有关联推荐记录的标题标记为已使用 */
    @Update("UPDATE tu_title_library t SET t.is_used = 1 WHERE t.is_deleted = 0 AND (t.is_used IS NULL OR t.is_used != 1) AND EXISTS (SELECT 1 FROM tu_title_recommendation r WHERE r.title_library_id = t.id)")
    int batchMarkUsedForMatched();

    /** 查询 platform 为空或空字符串的标题 */
    @Select("SELECT * FROM tu_title_library WHERE is_deleted = 0 AND (platform IS NULL OR platform = '')")
    List<TitleLibrary> findByEmptyPlatform();

    @Update("UPDATE tu_title_library SET platform = #{platform} WHERE id = #{id}")
    int updatePlatform(@Param("id") String id, @Param("platform") String platform);

    /** 查询所有未匹配用户的标题（用于重配）：排除已有 tu_title_recommendation 关联记录的标题 */
    @Select("SELECT t.*, t.track_id as trackId, tr.name as trackName FROM tu_title_library t " +
            "LEFT JOIN tu_track tr ON t.track_id = tr.id AND tr.is_deleted = 0 " +
            "WHERE t.is_deleted = 0 " +
            "AND NOT EXISTS (SELECT 1 FROM tu_title_recommendation r WHERE r.title_library_id = t.id)")
    List<TitleLibrary> findAllUnmatched();

    /** 临时：删除 track_id 指向已不存在的赛道的脏数据标题（逻辑删除） */
    @Update("UPDATE tu_title_library t SET t.is_deleted = 1 WHERE t.is_deleted = 0 AND t.track_id IS NOT NULL AND t.track_id != '' AND NOT EXISTS (SELECT 1 FROM tu_track tr WHERE tr.id = t.track_id AND tr.is_deleted = 0)")
    int deleteOrphanTitles();

    /** 修复脏数据1：已有关联推荐但 is_used != 1 的标题，批量标记为已使用 */
    @Update("UPDATE tu_title_library t SET t.is_used = 1 " +
            "WHERE t.is_deleted = 0 AND (t.is_used IS NULL OR t.is_used != 1) " +
            "AND EXISTS (SELECT 1 FROM tu_title_recommendation r WHERE r.title_library_id = t.id)")
    int fixIsUsedForMatched();

    /** 按赛道统计标题数量：总数、已使用、未使用（仅统计有赛道的） */
    @Select("SELECT t.track_id as trackId, tr.name as trackName, tr.platforms as platforms, " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN t.is_used = 1 THEN 1 ELSE 0 END) as usedCount, " +
            "SUM(CASE WHEN t.is_used IS NULL OR t.is_used != 1 THEN 1 ELSE 0 END) as unusedCount, " +
            "(SELECT COUNT(DISTINCT ut2.user_id) FROM tu_user_track ut2 INNER JOIN tu_user u2 ON ut2.user_id = u2.id WHERE u2.is_deleted = 0 AND ut2.track_id = t.track_id) as subscriberCount " +
            "FROM tu_title_library t " +
            "LEFT JOIN tu_track tr ON t.track_id = tr.id AND tr.is_deleted = 0 " +
            "WHERE t.is_deleted = 0 AND t.track_id IS NOT NULL AND t.track_id != '' " +
            "GROUP BY t.track_id, tr.name, tr.platforms " +
            "HAVING COUNT(*) > 0 " +
            "ORDER BY total DESC")
    List<Map<String, Object>> countByTrack();

    /** Agent: 查询用户某赛道近N天已推荐的标题 */
    @Select("SELECT t.id, t.title, t.track_id as trackId " +
            "FROM tu_title_library t " +
            "INNER JOIN tu_title_recommendation r ON t.id = r.title_library_id " +
            "WHERE r.user_id = #{userId} AND t.track_id = #{trackId} " +
            "AND r.recommend_date >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "AND t.is_deleted = 0 " +
            "ORDER BY r.recommend_date DESC, r.created_at DESC " +
            "LIMIT #{limit}")
    List<TitleLibrary> findRecentByUserAndTrack(@Param("userId") String userId, @Param("trackId") String trackId, @Param("days") int days, @Param("limit") int limit);

    /** Agent: 查询某赛道可用标题（未推荐、未使用、同赛道） */
    @Select("SELECT t.id, t.title, t.description, t.platform, t.track_id as trackId " +
            "FROM tu_title_library t " +
            "WHERE t.is_deleted = 0 AND t.track_id = #{trackId} " +
            "AND (t.is_used IS NULL OR t.is_used != 1) " +
            "AND NOT EXISTS (SELECT 1 FROM tu_title_recommendation r WHERE r.title_library_id = t.id) " +
            "ORDER BY t.created_at DESC " +
            "LIMIT #{limit}")
    List<TitleLibrary> findAvailableByTrack(@Param("trackId") String trackId, @Param("limit") int limit);
}
