package com.example.user.mapper;

import com.example.user.entity.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM tu_user WHERE is_deleted = 0 ORDER BY created_at DESC")
    List<User> findAll();

    @Select("SELECT id, username, password, status, phone, email, wx_id, ai_limit, track_limit as trackLimit, platform_limit as platformLimit, avatar, template, expire_date, last_login, remark, is_deleted, created_at, updated_at FROM tu_user WHERE id = #{id} AND is_deleted = 0")
    User findById(String id);

    @Select("SELECT * FROM tu_user WHERE username = #{username} AND is_deleted = 0")
    User findByUsername(String username);

    @Insert("INSERT INTO tu_user(id, username, password, status, phone, email, wx_id, ai_limit, track_limit, platform_limit, avatar, template, expire_date, last_login, remark, is_deleted) VALUES(#{id}, #{username}, #{password}, #{status}, #{phone}, #{email}, #{wxId}, #{aiLimit}, #{trackLimit}, #{platformLimit}, #{avatar}, #{template}, #{expireDate}, #{lastLogin}, #{remark}, 0)")
    int insert(User user);

    @Update("UPDATE tu_user SET username=#{username}, password=#{password}, status=#{status}, phone=#{phone}, email=#{email}, wx_id=#{wxId}, ai_limit=#{aiLimit}, track_limit=#{trackLimit}, platform_limit=#{platformLimit}, avatar=#{avatar}, template=#{template}, expire_date=#{expireDate}, last_login=#{lastLogin}, remark=#{remark} WHERE id=#{id}")
    int update(User user);

    @Update("UPDATE tu_user SET avatar=#{avatar} WHERE id=#{id}")
    int updateAvatar(@Param("id") String id, @Param("avatar") String avatar);

    @Update("UPDATE tu_user SET last_login=#{lastLogin} WHERE id=#{id}")
    int updateLastLogin(@Param("id") String id, @Param("lastLogin") java.time.LocalDateTime lastLogin);

    @Update("UPDATE tu_user SET password=#{password} WHERE id=#{id}")
    int updatePassword(@Param("id") String id, @Param("password") String password);

    @Update("UPDATE tu_user SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);
}
