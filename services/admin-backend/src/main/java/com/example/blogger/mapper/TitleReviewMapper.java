package com.example.blogger.mapper;

import com.example.blogger.entity.TitleReview;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface TitleReviewMapper {

    @Insert("INSERT INTO tu_title_review(id, title_library_id, review_status, review_reason, reviewed_by, reviewed_at, push_status, pushed_at, source, created_at) " +
            "VALUES(#{id}, #{titleLibraryId}, #{reviewStatus}, #{reviewReason}, #{reviewedBy}, #{reviewedAt}, #{pushStatus}, #{pushedAt}, #{source}, NOW())")
    int insert(TitleReview titleReview);

    @Update("UPDATE tu_title_review SET review_status=#{reviewStatus}, review_reason=#{reviewReason}, reviewed_by=#{reviewedBy}, reviewed_at=#{reviewedAt} WHERE id=#{id}")
    int updateReviewStatus(TitleReview titleReview);

    @Update("UPDATE tu_title_review SET push_status=#{pushStatus}, pushed_at=#{pushedAt} WHERE id=#{id}")
    int updatePushStatus(TitleReview titleReview);

    @Select("SELECT tr.*, t.title, t.description, t.platform, t.track_id as trackId, trk.name as trackName, t.use_count as useCount, t.is_used as isUsed " +
            "FROM tu_title_review tr " +
            "INNER JOIN tu_title_library t ON tr.title_library_id COLLATE utf8mb4_0900_ai_ci = t.id COLLATE utf8mb4_0900_ai_ci AND t.is_deleted = 0 " +
            "LEFT JOIN tu_track trk ON t.track_id COLLATE utf8mb4_0900_ai_ci = trk.id COLLATE utf8mb4_0900_ai_ci AND trk.is_deleted = 0 " +
            "WHERE tr.review_status = #{reviewStatus} " +
            "AND (#{platform} IS NULL OR #{platform} = '' OR t.platform = #{platform}) " +
            "AND (#{trackId} IS NULL OR #{trackId} = '' OR t.track_id = #{trackId}) " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' OR t.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (t.is_used IS NULL OR t.is_used != 1) " +
            "AND NOT EXISTS (SELECT 1 FROM tu_title_recommendation tr2 WHERE tr2.title_library_id = t.id AND tr2.subscription_post_id IS NOT NULL) " +
            "ORDER BY tr.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<TitleReview> findByReviewStatus(@Param("reviewStatus") String reviewStatus, @Param("platform") String platform,
                                          @Param("trackId") String trackId, @Param("keyword") String keyword,
                                          @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) " +
            "FROM tu_title_review tr " +
            "INNER JOIN tu_title_library t ON tr.title_library_id COLLATE utf8mb4_0900_ai_ci = t.id COLLATE utf8mb4_0900_ai_ci AND t.is_deleted = 0 " +
            "WHERE tr.review_status = #{reviewStatus} " +
            "AND (#{platform} IS NULL OR #{platform} = '' OR t.platform = #{platform}) " +
            "AND (#{trackId} IS NULL OR #{trackId} = '' OR t.track_id = #{trackId}) " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' OR t.title LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (t.is_used IS NULL OR t.is_used != 1) " +
            "AND NOT EXISTS (SELECT 1 FROM tu_title_recommendation tr2 WHERE tr2.title_library_id = t.id AND tr2.subscription_post_id IS NOT NULL)")
    int countByReviewStatus(@Param("reviewStatus") String reviewStatus, @Param("platform") String platform,
                            @Param("trackId") String trackId, @Param("keyword") String keyword);

    @Select("SELECT tr.*, t.title, t.description, t.platform, t.track_id as trackId, trk.name as trackName, t.use_count as useCount, t.is_used as isUsed " +
            "FROM tu_title_review tr " +
            "INNER JOIN tu_title_library t ON tr.title_library_id COLLATE utf8mb4_0900_ai_ci = t.id COLLATE utf8mb4_0900_ai_ci AND t.is_deleted = 0 " +
            "LEFT JOIN tu_track trk ON t.track_id COLLATE utf8mb4_0900_ai_ci = trk.id COLLATE utf8mb4_0900_ai_ci AND trk.is_deleted = 0 " +
            "WHERE tr.id = #{id}")
    TitleReview findById(String id);

    @Select("SELECT tr.*, t.title, t.description, t.platform, t.track_id as trackId, trk.name as trackName, t.use_count as useCount, t.is_used as isUsed " +
            "FROM tu_title_review tr " +
            "INNER JOIN tu_title_library t ON tr.title_library_id COLLATE utf8mb4_0900_ai_ci = t.id COLLATE utf8mb4_0900_ai_ci AND t.is_deleted = 0 " +
            "LEFT JOIN tu_track trk ON t.track_id COLLATE utf8mb4_0900_ai_ci = trk.id COLLATE utf8mb4_0900_ai_ci AND trk.is_deleted = 0 " +
            "WHERE tr.title_library_id = #{titleLibraryId}")
    TitleReview findByTitleLibraryId(String titleLibraryId);

    @Select("SELECT review_status as status, COUNT(*) as count FROM tu_title_review GROUP BY review_status")
    List<Map<String, Object>> countByStatus();

    @Select("SELECT tr.*, t.title, t.description, t.platform, t.track_id as trackId, trk.name as trackName, t.use_count as useCount, t.is_used as isUsed " +
            "FROM tu_title_review tr " +
            "INNER JOIN tu_title_library t ON tr.title_library_id COLLATE utf8mb4_0900_ai_ci = t.id COLLATE utf8mb4_0900_ai_ci AND t.is_deleted = 0 " +
            "LEFT JOIN tu_track trk ON t.track_id COLLATE utf8mb4_0900_ai_ci = trk.id COLLATE utf8mb4_0900_ai_ci AND trk.is_deleted = 0 " +
            "WHERE tr.review_status COLLATE utf8mb4_0900_ai_ci = 'approved' AND tr.push_status COLLATE utf8mb4_0900_ai_ci = 'unpushed' " +
            "ORDER BY tr.created_at DESC")
    List<TitleReview> findApprovedUnpushed();
}
