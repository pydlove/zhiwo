package com.example.blogger.mapper;

import com.example.blogger.entity.ScheduledPush;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ScheduledPushMapper {
    @Insert("INSERT INTO tu_scheduled_push(id, push_time, status, last_executed_date, user_filter_type, user_ids, created_by, created_at) " +
            "VALUES(#{id}, #{pushTime}, #{status}, #{lastExecutedDate}, #{userFilterType}, #{userIds}, #{createdBy}, NOW())")
    int insert(ScheduledPush push);

    @Select("SELECT * FROM tu_scheduled_push WHERE id = #{id}")
    ScheduledPush findById(String id);

    @Update("UPDATE tu_scheduled_push SET status = #{status}, last_executed_date = #{lastExecutedDate} WHERE id = #{id}")
    int updateStatus(@Param("id") String id, @Param("status") Integer status, @Param("lastExecutedDate") String lastExecutedDate);

    @Update("UPDATE tu_scheduled_push SET push_time = #{pushTime}, last_executed_date = NULL WHERE id = #{id}")
    int updatePushTime(@Param("id") String id, @Param("pushTime") String pushTime);

    @Delete("DELETE FROM tu_scheduled_push WHERE id = #{id}")
    int deleteById(@Param("id") String id);

    @Select("SELECT * FROM tu_scheduled_push WHERE status = 0 ORDER BY created_at ASC")
    List<ScheduledPush> findActive();

    @Select("SELECT * FROM tu_scheduled_push ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<ScheduledPush> findAll(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM tu_scheduled_push")
    int countAll();

    @Select("SELECT * FROM tu_scheduled_push WHERE status = #{status} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<ScheduledPush> findByStatus(@Param("status") Integer status, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM tu_scheduled_push WHERE status = #{status}")
    int countByStatus(@Param("status") Integer status);
}
