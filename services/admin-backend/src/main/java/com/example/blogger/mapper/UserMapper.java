package com.example.blogger.mapper;

import com.example.blogger.entity.User;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    @Select("SELECT id, username, status, phone, email, wx_id, wx_name as wxName, track_limit as trackLimit, platform_limit as platformLimit, template, expire_date as expireDate, last_login as lastLogin, membership_plan_id as membershipPlanId, invite_code as inviteCode, invited_by as invitedBy, is_real as isReal, created_at as createdAt FROM tu_user WHERE is_deleted = 0 ORDER BY created_at DESC")
    List<User> findAll();

    @Select("SELECT id, username, password, status, phone, email, wx_id, wx_name as wxName, ai_limit, track_limit as trackLimit, platform_limit as platformLimit, avatar, template, expire_date as expireDate, last_login as lastLogin, remark, can_set_email as canSetEmail, email_receive as emailReceive, membership_plan_id as membershipPlanId, invite_code as inviteCode, invited_by as invitedBy, is_real as isReal, is_deleted as isDeleted, created_at as createdAt, updated_at as updatedAt FROM tu_user WHERE id = #{id} AND is_deleted = 0")
    User findById(String id);

    @Select("SELECT id, username, password, status, phone, email, wx_id, wx_name as wxName, ai_limit, track_limit as trackLimit, platform_limit as platformLimit, avatar, template, expire_date as expireDate, last_login as lastLogin, remark, can_set_email as canSetEmail, email_receive as emailReceive, membership_plan_id as membershipPlanId, invite_code as inviteCode, invited_by as invitedBy, is_real as isReal, is_deleted as isDeleted, created_at as createdAt, updated_at as updatedAt FROM tu_user WHERE username = #{username} AND is_deleted = 0")
    User findByUsername(String username);

    @Insert("INSERT INTO tu_user(id, username, password, status, phone, email, wx_id, wx_name, ai_limit, track_limit, platform_limit, avatar, template, expire_date, last_login, remark, can_set_email, email_receive, membership_plan_id, invite_code, invited_by, is_real, is_deleted) VALUES(#{id}, #{username}, #{password}, #{status}, #{phone}, #{email}, #{wxId}, #{wxName}, #{aiLimit}, #{trackLimit}, #{platformLimit}, #{avatar}, #{template}, #{expireDate}, #{lastLogin}, #{remark}, #{canSetEmail}, #{emailReceive}, #{membershipPlanId}, #{inviteCode}, #{invitedBy}, #{isReal}, 0)")
    int insert(User user);

    @Update("UPDATE tu_user SET username=#{username}, password=#{password}, status=#{status}, phone=#{phone}, email=#{email}, wx_id=#{wxId}, wx_name=#{wxName}, ai_limit=#{aiLimit}, track_limit=#{trackLimit}, platform_limit=#{platformLimit}, avatar=#{avatar}, template=#{template}, expire_date=#{expireDate}, last_login=#{lastLogin}, remark=#{remark}, can_set_email=#{canSetEmail}, email_receive=#{emailReceive}, membership_plan_id=#{membershipPlanId}, invite_code=#{inviteCode}, invited_by=#{invitedBy}, is_real=#{isReal} WHERE id=#{id}")
    int update(User user);

    @Update("UPDATE tu_user SET last_login=#{lastLogin} WHERE id=#{id}")
    int updateLastLogin(@Param("id") String id, @Param("lastLogin") java.time.LocalDateTime lastLogin);

    @Update("UPDATE tu_user SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);

    @Update("UPDATE tu_user SET invite_code = #{inviteCode} WHERE id = #{id}")
    int updateInviteCode(@Param("id") String id, @Param("inviteCode") String inviteCode);

    @Select("SELECT id, username, password, status, phone, email, wx_id, wx_name as wxName, ai_limit, track_limit as trackLimit, platform_limit as platformLimit, avatar, template, expire_date as expireDate, last_login as lastLogin, remark, can_set_email as canSetEmail, email_receive as emailReceive, membership_plan_id as membershipPlanId, invite_code as inviteCode, invited_by as invitedBy, is_real as isReal, is_deleted as isDeleted, created_at as createdAt, updated_at as updatedAt FROM tu_user WHERE invite_code = #{inviteCode} AND is_deleted = 0")
    User findByInviteCode(String inviteCode);

    @Select("SELECT COUNT(*) FROM tu_user WHERE is_deleted = 0")
    int countUsers();

    @Select("SELECT id, username, phone, email, wx_id, is_real as isReal FROM tu_user WHERE status = 1 AND is_deleted = 0 ORDER BY created_at DESC")
    List<Map<String, Object>> findAllActiveUsers();

    @Select("SELECT id, username, email FROM tu_user WHERE status = 1 AND is_deleted = 0 AND is_real = 1 AND email IS NOT NULL AND email != '' ORDER BY created_at DESC")
    List<Map<String, Object>> findAllActiveUsersWithEmail();
}
