package com.example.blogger.service;

import com.example.blogger.entity.MembershipPlan;
import com.example.blogger.entity.Order;
import com.example.blogger.entity.User;
import com.example.blogger.mapper.OrderMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderMapper orderMapper;

    public OrderService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    public List<Order> search(String userId, String type, String planId, String dateStart, String dateEnd, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return orderMapper.search(userId, type, planId, dateStart, dateEnd, pageSize, offset);
    }

    public int countSearch(String userId, String type, String planId, String dateStart, String dateEnd) {
        return orderMapper.countSearch(userId, type, planId, dateStart, dateEnd);
    }

    public Order getById(String id) {
        return orderMapper.findById(id);
    }

    public void save(Order order) {
        if (order.getId() == null || order.getId().isEmpty()) {
            order.setId(UUID.randomUUID().toString().replace("-", ""));
            orderMapper.insert(order);
        }
    }

    public void createAutoOrder(User user, MembershipPlan plan, String type) {
        if (user == null || plan == null) return;
        Order order = new Order();
        order.setUserId(user.getId());
        order.setUserName(user.getUsername());
        order.setPlanId(plan.getId());
        order.setPlanName(plan.getName());
        order.setType(type);
        order.setAmount(plan.getPrice() != null ? plan.getPrice() : BigDecimal.ZERO);
        order.setRemark("auto");
        save(order);
    }

    public void updateAmount(String id, BigDecimal amount) {
        if (id == null || id.isEmpty()) {
            throw new RuntimeException("订单ID不能为空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("金额不能为负数");
        }
        Order order = orderMapper.findById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        orderMapper.updateAmount(id, amount);
    }

    public void refund(String id, BigDecimal refundAmount) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("退单金额必须大于0");
        }
        if (order.getAmount() != null && refundAmount.compareTo(order.getAmount()) > 0) {
            throw new RuntimeException("退单金额不能大于订单金额");
        }
        orderMapper.refund(id, refundAmount);
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        result.put("todayAmount", orderMapper.sumToday());
        result.put("monthAmount", orderMapper.sumThisMonth());
        result.put("yearAmount", orderMapper.sumThisYear());
        result.put("totalAmount", orderMapper.sumTotal());
        result.put("orderCount", orderMapper.countAll());
        result.put("byPlan", orderMapper.statsByPlan());
        result.put("refundToday", orderMapper.sumRefundToday());
        result.put("refundMonth", orderMapper.sumRefundThisMonth());
        result.put("refundTotal", orderMapper.sumRefundTotal());
        return result;
    }

    public void delete(String id) {
        orderMapper.deleteById(id);
    }
}
