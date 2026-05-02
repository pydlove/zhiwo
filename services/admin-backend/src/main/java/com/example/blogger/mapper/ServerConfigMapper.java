package com.example.blogger.mapper;

import com.example.blogger.entity.ServerConfig;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ServerConfigMapper {

    @Insert("INSERT INTO tu_server_config(id, name, host, port, is_active, is_default, created_at) " +
            "VALUES(#{id}, #{name}, #{host}, #{port}, #{isActive}, #{isDefault}, NOW())")
    int insert(ServerConfig serverConfig);

    @Update("UPDATE tu_server_config SET name=#{name}, host=#{host}, port=#{port}, " +
            "is_active=#{isActive}, is_default=#{isDefault} WHERE id=#{id}")
    int update(ServerConfig serverConfig);

    @Delete("DELETE FROM tu_server_config WHERE id = #{id}")
    int delete(String id);

    @Select("SELECT * FROM tu_server_config WHERE (is_active = 1 OR is_active IS NULL) ORDER BY created_at DESC")
    List<ServerConfig> findAllActive();

    @Select("SELECT * FROM tu_server_config ORDER BY created_at DESC")
    List<ServerConfig> findAll();

    @Select("SELECT * FROM tu_server_config WHERE id = #{id}")
    ServerConfig findById(String id);

    @Select("SELECT * FROM tu_server_config WHERE is_default = 1 AND (is_active = 1 OR is_active IS NULL) LIMIT 1")
    ServerConfig findDefault();

    @Update("UPDATE tu_server_config SET is_default = 0 WHERE is_default = 1")
    int clearDefault();
}
