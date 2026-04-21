package com.example.blogger.mapper;

import com.example.blogger.entity.Role;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RoleMapper {
    @Select("SELECT * FROM ta_role WHERE is_deleted = 0 ORDER BY created_at DESC")
    List<Role> findAll();

    @Select("SELECT * FROM ta_role WHERE id = #{id} AND is_deleted = 0")
    Role findById(String id);

    @Select("SELECT * FROM ta_role WHERE name = #{name} AND is_deleted = 0")
    Role findByName(String name);

    @Insert("INSERT INTO ta_role(id, name, permissions, is_deleted) VALUES(#{id}, #{name}, #{permissions}, 0)")
    int insert(Role role);

    @Update("UPDATE ta_role SET name=#{name}, permissions=#{permissions} WHERE id=#{id}")
    int update(Role role);

    @Update("UPDATE ta_role SET is_deleted = 1 WHERE id = #{id}")
    int delete(String id);

    @Select("SELECT COUNT(*) FROM ta_user WHERE role = #{roleName} AND is_deleted = 0")
    int countAdminsByRole(String roleName);
}
