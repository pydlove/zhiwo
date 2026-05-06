package com.example.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface OperatorMapper {

    @Select("SELECT id, username, name, qr_code_url as qrCodeUrl, role FROM ta_user WHERE username = #{username} AND role = '运营管理员' AND is_deleted = 0")
    Map<String, Object> findOperatorByUsername(@Param("username") String username);
}
