package com.example.blogger.mapper;

import com.example.blogger.entity.Order;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    @Select("<script>" +
            "SELECT o.* FROM tu_order o " +
            "WHERE 1=1 " +
            "<if test='userId != null'> AND o.user_id = #{userId} </if>" +
            "<if test='type != null'> AND o.type = #{type} </if>" +
            "<if test='planId != null'> AND o.plan_id = #{planId} </if>" +
            "<if test='dateStart != null'> AND DATE(o.created_at) &gt;= #{dateStart} </if>" +
            "<if test='dateEnd != null'> AND DATE(o.created_at) &lt;= #{dateEnd} </if>" +
            "ORDER BY o.created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<Order> search(@Param("userId") String userId, @Param("type") String type,
                       @Param("planId") String planId, @Param("dateStart") String dateStart,
                       @Param("dateEnd") String dateEnd, @Param("limit") int limit, @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM tu_order o " +
            "WHERE 1=1 " +
            "<if test='userId != null'> AND o.user_id = #{userId} </if>" +
            "<if test='type != null'> AND o.type = #{type} </if>" +
            "<if test='planId != null'> AND o.plan_id = #{planId} </if>" +
            "<if test='dateStart != null'> AND DATE(o.created_at) &gt;= #{dateStart} </if>" +
            "<if test='dateEnd != null'> AND DATE(o.created_at) &lt;= #{dateEnd} </if>" +
            "</script>")
    int countSearch(@Param("userId") String userId, @Param("type") String type,
                    @Param("planId") String planId, @Param("dateStart") String dateStart,
                    @Param("dateEnd") String dateEnd);

    @Select("SELECT * FROM tu_order WHERE id = #{id}")
    Order findById(String id);

    @Insert("INSERT INTO tu_order(id, user_id, user_name, plan_id, plan_name, type, amount, remark, created_at) " +
            "VALUES(#{id}, #{userId}, #{userName}, #{planId}, #{planName}, #{type}, #{amount}, #{remark}, NOW())")
    int insert(Order order);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM tu_order WHERE DATE(created_at) = CURDATE()")
    BigDecimal sumToday();

    @Select("SELECT COALESCE(SUM(amount), 0) FROM tu_order WHERE created_at >= DATE_FORMAT(NOW(), '%Y-%m-01')")
    BigDecimal sumThisMonth();

    @Select("SELECT COALESCE(SUM(amount), 0) FROM tu_order WHERE created_at >= DATE_FORMAT(NOW(), '%Y-01-01')")
    BigDecimal sumThisYear();

    @Select("SELECT COALESCE(SUM(amount), 0) FROM tu_order")
    BigDecimal sumTotal();

    @Select("SELECT COUNT(*) FROM tu_order")
    int countAll();

    @Select("SELECT plan_name as planName, COUNT(*) as count, COALESCE(SUM(amount), 0) as amount " +
            "FROM tu_order GROUP BY plan_id, plan_name ORDER BY amount DESC")
    List<Map<String, Object>> statsByPlan();

    @Select("SELECT * FROM tu_order WHERE user_id = #{userId} AND type = #{type} ORDER BY created_at DESC LIMIT 1")
    Order findLatestByUserAndType(@Param("userId") String userId, @Param("type") String type);
}
