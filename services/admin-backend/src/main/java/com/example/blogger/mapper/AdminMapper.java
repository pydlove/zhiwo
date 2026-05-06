package com.example.blogger.mapper;

import com.example.blogger.entity.Admin;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AdminMapper {
    @Select("SELECT * FROM ta_user WHERE is_deleted = 0 ORDER BY created_at DESC")
    List<Admin> findAll();

    @Select("SELECT * FROM ta_user WHERE id = #{id} AND is_deleted = 0")
    Admin findById(String id);

    @Select("SELECT * FROM ta_user WHERE username = #{username} AND is_deleted = 0")
    Admin findByUsername(String username);

    @Insert("INSERT INTO ta_user(id, username, password, status, phone, email, wx_id, ai_limit, expire_date, last_login, remark, name, role, qr_code_url, is_deleted) " +
            "VALUES(#{id}, #{username}, #{password}, #{status}, #{phone}, #{email}, #{wxId}, #{aiLimit}, #{expireDate}, #{lastLogin}, #{remark}, #{name}, #{role}, #{qrCodeUrl}, 0)")
    int insert(Admin admin);

    @Update("UPDATE ta_user SET username=#{username}, password=#{password}, status=#{status}, phone=#{phone}, email=#{email}, wx_id=#{wxId}, " +
            "ai_limit=#{aiLimit}, expire_date=#{expireDate}, last_login=#{lastLogin}, remark=#{remark}, name=#{name}, role=#{role}, qr_code_url=#{qrCodeUrl} WHERE id=#{id}")
    int update(Admin admin);

    @Update("UPDATE ta_user SET last_login=#{lastLogin} WHERE id=#{id}")
    int updateLastLogin(@Param("id") String id, @Param("lastLogin") java.time.LocalDateTime lastLogin);

    @Update("UPDATE ta_user SET password=#{password} WHERE id=#{id}")
    int updatePassword(@Param("id") String id, @Param("password") String password);

    @Update("UPDATE ta_user SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);
}
