package com.example.blogger.controller;

import com.example.blogger.entity.MembershipPlan;
import com.example.blogger.entity.Order;
import com.example.blogger.entity.Result;
import com.example.blogger.entity.User;
import com.example.blogger.mapper.MembershipPlanMapper;
import com.example.blogger.mapper.UserMapper;
import com.example.blogger.service.OrderService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;
    private final MembershipPlanMapper membershipPlanMapper;
    private final UserMapper userMapper;

    public OrderController(OrderService orderService, MembershipPlanMapper membershipPlanMapper, UserMapper userMapper) {
        this.orderService = orderService;
        this.membershipPlanMapper = membershipPlanMapper;
        this.userMapper = userMapper;
    }

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "planId", required = false) String planId,
            @RequestParam(value = "dateStart", required = false) String dateStart,
            @RequestParam(value = "dateEnd", required = false) String dateEnd,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        List<Order> list = orderService.search(userId, type, planId, dateStart, dateEnd, page, pageSize);
        int total = orderService.countSearch(userId, type, planId, dateStart, dateEnd);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return Result.ok(result);
    }

    @PostMapping
    public Result<Void> save(@RequestBody Order order) {
        if (order.getAmount() == null) {
            MembershipPlan plan = membershipPlanMapper.findById(order.getPlanId());
            if (plan != null && plan.getPrice() != null) {
                order.setAmount(plan.getPrice());
                order.setPlanName(plan.getName());
            }
        }
        if (order.getUserId() != null && (order.getUserName() == null || order.getUserName().isEmpty())) {
            User user = userMapper.findById(order.getUserId());
            if (user != null) {
                order.setUserName(user.getUsername());
            }
        }
        orderService.save(order);
        return Result.ok(null);
    }

    @PostMapping("/refund")
    public Result<Void> refund(@RequestBody Map<String, Object> req) {
        String id = req.get("id") != null ? req.get("id").toString() : "";
        BigDecimal refundAmount = null;
        Object amtObj = req.get("refundAmount");
        if (amtObj instanceof Number) {
            refundAmount = BigDecimal.valueOf(((Number) amtObj).doubleValue());
        } else if (amtObj != null) {
            try {
                refundAmount = new BigDecimal(amtObj.toString());
            } catch (Exception e) {
                return Result.error("退单金额格式错误");
            }
        }
        if (id.isEmpty()) {
            return Result.error("订单ID不能为空");
        }
        if (refundAmount == null) {
            return Result.error("退单金额不能为空");
        }
        try {
            orderService.refund(id, refundAmount);
            return Result.ok(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/amount")
    public Result<Void> updateAmount(@RequestBody Map<String, Object> req) {
        String id = req.get("id") != null ? req.get("id").toString() : "";
        if (id.isEmpty()) {
            return Result.error("订单ID不能为空");
        }
        BigDecimal amount = null;
        Object amtObj = req.get("amount");
        if (amtObj instanceof Number) {
            amount = BigDecimal.valueOf(((Number) amtObj).doubleValue());
        } else if (amtObj != null) {
            try {
                amount = new BigDecimal(amtObj.toString());
            } catch (Exception e) {
                return Result.error("金额格式错误");
            }
        }
        try {
            orderService.updateAmount(id, amount);
            return Result.ok(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(orderService.stats());
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        orderService.delete(id);
        return Result.ok(null);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "planId", required = false) String planId,
            @RequestParam(value = "dateStart", required = false) String dateStart,
            @RequestParam(value = "dateEnd", required = false) String dateEnd) {
        try {
            List<Order> list = orderService.search(userId, type, planId, dateStart, dateEnd, 1, 10000);
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet("订单列表");
            String[] headers = {"用户名", "套餐", "类型", "金额", "退单金额", "备注", "创建时间"};

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            Map<String, String> typeMap = Map.of(
                "open_account", "开户",
                "renew", "续费",
                "upgrade", "升级"
            );

            for (int i = 0; i < list.size(); i++) {
                Order o = list.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(o.getUserName() != null ? o.getUserName() : "");
                row.createCell(1).setCellValue(o.getPlanName() != null ? o.getPlanName() : "");
                row.createCell(2).setCellValue(typeMap.getOrDefault(o.getType(), o.getType()));
                row.createCell(3).setCellValue(o.getAmount() != null ? o.getAmount().doubleValue() : 0);
                row.createCell(4).setCellValue(o.getRefundAmount() != null ? o.getRefundAmount().doubleValue() : 0);
                row.createCell(5).setCellValue(o.getRemark() != null ? o.getRemark() : "");
                row.createCell(6).setCellValue(o.getCreatedAt() != null ? o.getCreatedAt().toString() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            wb.close();

            String fileName = "orders_export_" + System.currentTimeMillis() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(out.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
