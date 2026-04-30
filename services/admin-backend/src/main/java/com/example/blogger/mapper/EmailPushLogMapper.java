package com.example.blogger.mapper;

import com.example.blogger.entity.EmailPushLog;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;

@Mapper
public interface EmailPushLogMapper {

    @Insert("INSERT INTO tu_email_push_log(id, user_id, push_date, type, title_library_id, created_at) " +
            "VALUES(#{id}, #{userId}, #{pushDate}, #{type}, #{titleLibraryId}, NOW())")
    int insert(EmailPushLog log);

    @Select("SELECT COUNT(*) FROM tu_email_push_log WHERE user_id = #{userId} AND push_date = #{date}")
    int countByUserAndDate(@Param("userId") String userId, @Param("date") LocalDate date);
}
