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

    @Insert("INSERT INTO tu_order(id, user_id, user_name, plan_id, plan_name, type, amount, refund_amount, remark, created_at) " +
            "VALUES(#{id}, #{userId}, #{userName}, #{planId}, #{planName}, #{type}, #{amount}, 0, #{remark}, NOW())")
    int insert(Order order);

    @Update("UPDATE tu_order SET refund_amount = #{refundAmount}, remark = CONCAT(IFNULL(remark, ''), ' [退单]', ' 退单金额:', #{refundAmount}) WHERE id = #{id}")
    int refund(@Param("id") String id, @Param("refundAmount") BigDecimal refundAmount);

    @Update("UPDATE tu_order SET amount = #{amount} WHERE id = #{id}")
    int updateAmount(@Param("id") String id, @Param("amount") BigDecimal amount);

    // 收益统计：只统计未退单的（refund_amount = 0）
    @Select("SELECT COALESCE(SUM(amount), 0) FROM tu_order WHERE DATE(created_at) = CURDATE() AND (refund_amount IS NULL OR refund_amount = 0)")
    BigDecimal sumToday();

    @Select("SELECT COALESCE(SUM(amount), 0) FROM tu_order WHERE created_at >= DATE_FORMAT(NOW(), '%Y-%m-01') AND (refund_amount IS NULL OR refund_amount = 0)")
    BigDecimal sumThisMonth();

    @Select("SELECT COALESCE(SUM(amount), 0) FROM tu_order WHERE created_at >= DATE_FORMAT(NOW(), '%Y-01-01') AND (refund_amount IS NULL OR refund_amount = 0)")
    BigDecimal sumThisYear();

    @Select("SELECT COALESCE(SUM(amount), 0) FROM tu_order")
    BigDecimal sumTotal();

    // 退单金额统计
    @Select("SELECT COALESCE(SUM(refund_amount), 0) FROM tu_order WHERE DATE(created_at) = CURDATE() AND refund_amount > 0")
    BigDecimal sumRefundToday();

    @Select("SELECT COALESCE(SUM(refund_amount), 0) FROM tu_order WHERE created_at >= DATE_FORMAT(NOW(), '%Y-%m-01') AND refund_amount > 0")
    BigDecimal sumRefundThisMonth();

    @Select("SELECT COALESCE(SUM(refund_amount), 0) FROM tu_order WHERE refund_amount > 0")
    BigDecimal sumRefundTotal();

    @Select("SELECT COUNT(*) FROM tu_order")
    int countAll();

    @Select("SELECT plan_name as planName, COUNT(*) as count, COALESCE(SUM(amount), 0) as amount " +
            "FROM tu_order GROUP BY plan_id, plan_name ORDER BY amount DESC")
    List<Map<String, Object>> statsByPlan();

    @Select("SELECT * FROM tu_order WHERE user_id = #{userId} AND type = #{type} ORDER BY created_at DESC LIMIT 1")
    Order findLatestByUserAndType(@Param("userId") String userId, @Param("type") String type);

    @Delete("DELETE FROM tu_order WHERE id = #{id}")
    int deleteById(String id);
}
