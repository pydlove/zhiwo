package com.example.blogger.controller;

import com.example.blogger.entity.MembershipPlan;
import com.example.blogger.entity.Result;
import com.example.blogger.entity.User;
import com.example.blogger.entity.UserTrack;
import com.example.blogger.service.MembershipPlanService;
import com.example.blogger.service.UserService;
import com.example.blogger.service.UserTrackService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;
    private final UserTrackService userTrackService;
    private final MembershipPlanService membershipPlanService;

    public UserController(UserService userService, UserTrackService userTrackService, MembershipPlanService membershipPlanService) {
        this.userService = userService;
        this.userTrackService = userTrackService;
        this.membershipPlanService = membershipPlanService;
    }

    private void syncPlanLimits(User user) {
        syncPlanLimits(user, null);
    }

    private void syncPlanLimits(User user, String oldPlanId) {
        String planId = user.getMembershipPlanId();
        if (planId == null || planId.isEmpty()) {
            return;
        }
        MembershipPlan plan = membershipPlanService.getById(planId);
        if (plan == null) {
            return;
        }
        boolean planChanged = oldPlanId != null && !oldPlanId.equals(planId);
        // 当套餐变更时强制同步；否则仅在字段为空时同步套餐默认值（允许管理员手动覆盖）
        if (planChanged || user.getTrackLimit() == null || user.getTrackLimit() <= 0) {
            user.setTrackLimit(plan.getTrackLimit());
        }
        if (planChanged || user.getAiLimit() == null || user.getAiLimit() <= 0) {
            user.setAiLimit(plan.getAiLimit());
        }
        if (planChanged || user.getPlatformLimit() == null || user.getPlatformLimit().isEmpty()) {
            user.setPlatformLimit(plan.getPlatformLimit());
        }
        if (planChanged || (user.getExpireDate() == null && plan.getExpireDays() != null && plan.getExpireDays() > 0)) {
            user.setExpireDate(LocalDate.now().plusDays(plan.getExpireDays()));
        }
    }

    @GetMapping
    public Result<List<User>> list() {
        List<User> users = userService.list();
        for (User u : users) {
            List<UserTrack> tracks = userTrackService.listByUser(u.getId());
            u.setTrackIds(tracks.stream().map(UserTrack::getTrackId).collect(Collectors.toList()));
        }
        return Result.ok(users);
    }

    @GetMapping("/{id}")
    public Result<User> get(@PathVariable String id) {
        return Result.ok(userService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody User user) {
        syncPlanLimits(user);
        userService.save(user);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody User user) {
        User existing = userService.getById(id);
        if (existing == null) {
            return Result.error("用户不存在");
        }
        String oldPlanId = existing.getMembershipPlanId();
        if (user.getUsername() != null) existing.setUsername(user.getUsername());
        if (user.getPassword() != null) existing.setPassword(user.getPassword());
        if (user.getStatus() != null) existing.setStatus(user.getStatus());
        if (user.getPhone() != null) existing.setPhone(user.getPhone());
        if (user.getEmail() != null) existing.setEmail(user.getEmail());
        if (user.getWxId() != null) existing.setWxId(user.getWxId());
        if (user.getAiLimit() != null) existing.setAiLimit(user.getAiLimit());
        if (user.getTrackLimit() != null) existing.setTrackLimit(user.getTrackLimit());
        if (user.getPlatformLimit() != null) existing.setPlatformLimit(user.getPlatformLimit());
        if (user.getAvatar() != null) existing.setAvatar(user.getAvatar());
        if (user.getTemplate() != null) existing.setTemplate(user.getTemplate());
        if (user.getExpireDate() != null) existing.setExpireDate(user.getExpireDate());
        if (user.getRemark() != null) existing.setRemark(user.getRemark());
        if (user.getCanSetEmail() != null) existing.setCanSetEmail(user.getCanSetEmail());
        if (user.getEmailReceive() != null) existing.setEmailReceive(user.getEmailReceive());
        if (user.getMembershipPlanId() != null) existing.setMembershipPlanId(user.getMembershipPlanId());
        if (user.getInviteCode() != null) existing.setInviteCode(user.getInviteCode());
        if (user.getInvitedBy() != null) existing.setInvitedBy(user.getInvitedBy());
        syncPlanLimits(existing, oldPlanId);
        userService.save(existing);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        userService.delete(id);
        return Result.ok(null);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) List<String> userIds) {
        try {
            List<User> users = userService.list();
            List<User> filtered = new ArrayList<>();
            for (User u : users) {
                if (userIds != null && !userIds.isEmpty() && !userIds.contains(u.getId())) continue;
                if (status != null && !status.equals(u.getStatus())) continue;
                if (keyword != null && !keyword.isEmpty()) {
                    String name = u.getUsername() != null ? u.getUsername() : "";
                    String phone = u.getPhone() != null ? u.getPhone() : "";
                    String email = u.getEmail() != null ? u.getEmail() : "";
                    String wxId = u.getWxId() != null ? u.getWxId() : "";
                    if (!name.contains(keyword) && !phone.contains(keyword) && !email.contains(keyword) && !wxId.contains(keyword)) continue;
                }
                filtered.add(u);
            }

            Map<String, String> planMap = new HashMap<>();
            for (MembershipPlan p : membershipPlanService.list()) {
                planMap.put(p.getId(), p.getName());
            }

            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet("用户列表");

            Row header = sheet.createRow(0);
            String[] cols = {"用户名", "手机号", "邮箱", "微信号", "可选赛道数", "可访问平台", "到期时间", "会员套餐", "默认样式", "状态", "备注"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
            }

            for (int i = 0; i < filtered.size(); i++) {
                User u = filtered.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(u.getUsername() != null ? u.getUsername() : "");
                row.createCell(1).setCellValue(u.getPhone() != null ? u.getPhone() : "");
                row.createCell(2).setCellValue(u.getEmail() != null ? u.getEmail() : "");
                row.createCell(3).setCellValue(u.getWxId() != null ? u.getWxId() : "");
                row.createCell(4).setCellValue(u.getTrackLimit() != null ? u.getTrackLimit() : 0);
                row.createCell(5).setCellValue(u.getPlatformLimit() != null ? u.getPlatformLimit() : "");
                row.createCell(6).setCellValue(u.getExpireDate() != null ? u.getExpireDate().toString() : "");
                row.createCell(7).setCellValue(planMap.getOrDefault(u.getMembershipPlanId(), ""));
                row.createCell(8).setCellValue(u.getTemplate() != null ? u.getTemplate() : "");
                row.createCell(9).setCellValue(u.getStatus() != null && u.getStatus() == 1 ? "正常" : "已禁用");
                row.createCell(10).setCellValue(u.getRemark() != null ? u.getRemark() : "");
            }

            for (int i = 0; i < cols.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            wb.close();

            String fileName = "user_export_" + System.currentTimeMillis() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(out.toByteArray());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importUsers(@RequestParam("excel") MultipartFile excel) {
        try {
            Workbook wb = new XSSFWorkbook(excel.getInputStream());
            Sheet sheet = wb.getSheetAt(0);
            List<String> errors = new ArrayList<>();
            int success = 0;
            int skip = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String username = getCellString(row, 0);
                String phone = getCellString(row, 1);
                String email = getCellString(row, 2);
                String wxId = getCellString(row, 3);
                String trackLimitStr = getCellString(row, 4);
                String platformLimit = getCellString(row, 5);
                String expireDate = getCellString(row, 6);
                String membershipPlanName = getCellString(row, 7);
                String template = getCellString(row, 8);
                String statusStr = getCellString(row, 9);
                String remark = getCellString(row, 10);

                if (username == null || username.isEmpty()) {
                    skip++;
                    continue;
                }

                // Check duplicate by username
                User existing = userService.list().stream()
                        .filter(u -> username.equals(u.getUsername()))
                        .findFirst()
                        .orElse(null);

                User user = new User();
                user.setUsername(username);
                user.setPhone(phone != null ? phone : "");
                user.setEmail(email != null ? email : "");
                user.setWxId(wxId != null ? wxId : "");
                user.setTrackLimit(parseIntSafe(trackLimitStr, 0));
                user.setPlatformLimit(platformLimit != null ? platformLimit : "公众号");
                user.setExpireDate(parseDateSafe(expireDate));
                user.setTemplate(template != null && !template.isEmpty() ? template : "基础风格");
                user.setRemark(remark != null ? remark : "");
                user.setPassword("Abc123456");
                user.setCanSetEmail(0);
                user.setEmailReceive(0);

                // Map membership plan name to id
                if (membershipPlanName != null && !membershipPlanName.isEmpty()) {
                    for (MembershipPlan p : membershipPlanService.list()) {
                        if (membershipPlanName.equals(p.getName())) {
                            user.setMembershipPlanId(p.getId());
                            break;
                        }
                    }
                }

                if (existing != null) {
                    user.setId(existing.getId());
                    user.setPassword(existing.getPassword());
                    user.setCanSetEmail(existing.getCanSetEmail());
                    user.setEmailReceive(existing.getEmailReceive());
                    // 已有用户：如果导入文件未指定状态，保留原状态；否则按导入值更新
                    if (statusStr == null || statusStr.isEmpty()) {
                        user.setStatus(existing.getStatus() != null ? existing.getStatus() : 1);
                    } else {
                        user.setStatus("正常".equals(statusStr) || "1".equals(statusStr) ? 1 : 0);
                    }
                } else {
                    // 新用户：未指定状态则默认正常
                    user.setStatus(statusStr == null || statusStr.isEmpty() || "正常".equals(statusStr) || "1".equals(statusStr) ? 1 : 0);
                }

                userService.save(user);
                success++;
            }
            wb.close();

            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("skip", skip);
            result.put("errors", errors);
            return Result.ok(result);

        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) {
            String v = cell.getStringCellValue();
            return v != null ? v.trim() : null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
            }
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return cell.toString().trim();
    }

    private int parseIntSafe(String val, int defaultVal) {
        if (val == null || val.isEmpty()) return defaultVal;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private LocalDate parseDateSafe(String val) {
        if (val == null || val.isEmpty()) return LocalDate.now().plusYears(1);
        try {
            return LocalDate.parse(val);
        } catch (Exception e) {
            return LocalDate.now().plusYears(1);
        }
    }
}
