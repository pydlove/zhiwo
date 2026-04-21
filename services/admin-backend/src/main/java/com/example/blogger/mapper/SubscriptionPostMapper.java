package com.example.blogger.mapper;

import com.example.blogger.entity.SubscriptionPost;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;
import java.util.List;
import java.util.Map;

@Mapper
public interface SubscriptionPostMapper {
    @Select("SELECT * FROM tu_subscription_post WHERE is_deleted = 0 ORDER BY created_at DESC")
    List<SubscriptionPost> findAll();

    @Select("SELECT * FROM tu_subscription_post WHERE id = #{id} AND is_deleted = 0")
    SubscriptionPost findById(String id);

    @Select("SELECT * FROM tu_subscription_post WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<SubscriptionPost> findByUserId(String userId);

    @Select("SELECT * FROM tu_subscription_post WHERE user_id = #{userId} AND track_id = #{trackId} AND is_deleted = 0 AND status = '已上架' ORDER BY created_at DESC LIMIT 1")
    SubscriptionPost findLatestByUserAndTrack(@Param("userId") String userId, @Param("trackId") String trackId);

    @SelectProvider(type = SqlProvider.class, method = "findByCondition")
    List<SubscriptionPost> findByCondition(Map<String, Object> params);

    @Insert("INSERT INTO tu_subscription_post(id, user_id, track_id, title, description, file_url, file_name, status, is_deleted) VALUES(#{id}, #{userId}, #{trackId}, #{title}, #{description}, #{fileUrl}, #{fileName}, #{status}, 0)")
    int insert(SubscriptionPost p);

    @Update("UPDATE tu_subscription_post SET user_id=#{userId}, track_id=#{trackId}, title=#{title}, description=#{description}, file_url=#{fileUrl}, file_name=#{fileName}, status=#{status} WHERE id=#{id}")
    int update(SubscriptionPost p);

    @Update("UPDATE tu_subscription_post SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);

    class SqlProvider {
        public String findByCondition(Map<String, Object> params) {
            return new SQL() {{
                SELECT("*");
                FROM("tu_subscription_post");
                WHERE("is_deleted = 0");
                if (params.get("userId") != null && !((String) params.get("userId")).isEmpty()) {
                    WHERE("user_id = #{userId}");
                }
                if (params.get("trackId") != null && !((String) params.get("trackId")).isEmpty()) {
                    WHERE("track_id = #{trackId}");
                }
                if (params.get("status") != null && !((String) params.get("status")).isEmpty()) {
                    WHERE("status = #{status}");
                }
                if (params.get("keyword") != null && !((String) params.get("keyword")).isEmpty()) {
                    String kw = (String) params.get("keyword");
                    WHERE("(title LIKE '%" + kw + "%' OR description LIKE '%" + kw + "%' OR file_name LIKE '%" + kw + "%')");
                }
                if (params.get("startDate") != null && !((String) params.get("startDate")).isEmpty()) {
                    WHERE("created_at >= #{startDate}");
                }
                if (params.get("endDate") != null && !((String) params.get("endDate")).isEmpty()) {
                    WHERE("created_at < DATE_ADD(#{endDate}, INTERVAL 1 DAY)");
                }
                ORDER_BY("created_at DESC");
            }}.toString();
        }
    }
}
