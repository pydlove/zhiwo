package com.example.blogger.controller;

import com.example.blogger.entity.MembershipPlan;
import com.example.blogger.entity.Result;
import com.example.blogger.entity.TitleLibrary;
import com.example.blogger.entity.TitleRecommendation;
import com.example.blogger.entity.Track;
import com.example.blogger.entity.User;
import com.example.blogger.entity.UserTrack;
import com.example.blogger.mapper.TitleLibraryMapper;
import com.example.blogger.mapper.TitleRecommendationMapper;
import com.example.blogger.mapper.TrackMapper;
import com.example.blogger.mapper.UserTrackMapper;
import com.example.blogger.service.MembershipPlanService;
import com.example.blogger.service.OrderService;
import com.example.blogger.service.TitleLibraryService;
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
    private final UserTrackMapper userTrackMapper;
    private final TitleLibraryService titleLibraryService;
    private final TitleLibraryMapper titleLibraryMapper;
    private final TitleRecommendationMapper titleRecommendationMapper;
    private final TrackMapper trackMapper;
    private final OrderService orderService;

    public UserController(UserService userService, UserTrackService userTrackService, MembershipPlanService membershipPlanService,
                          UserTrackMapper userTrackMapper, TitleLibraryService titleLibraryService,
                          TitleLibraryMapper titleLibraryMapper, TitleRecommendationMapper titleRecommendationMapper,
                          TrackMapper trackMapper, OrderService orderService) {
        this.userService = userService;
        this.userTrackService = userTrackService;
        this.membershipPlanService = membershipPlanService;
        this.userTrackMapper = userTrackMapper;
        this.titleLibraryService = titleLibraryService;
        this.titleLibraryMapper = titleLibraryMapper;
        this.titleRecommendationMapper = titleRecommendationMapper;
        this.trackMapper = trackMapper;
        this.orderService = orderService;
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
        // 可选赛道数由管理员自由编辑，不再根据套餐自动同步
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
    public Result<List<User>> list(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "platform", required = false) String platform,
            @RequestParam(value = "trackId", required = false) String trackId,
            @RequestParam(value = "userType", required = false) Integer userType) {
        List<User> users = userService.list();

        // 批量查询所有用户赛道，避免 N+1
        Map<String, List<String>> trackMap = new HashMap<>();
        if (!users.isEmpty()) {
            List<String> userIds = users.stream().map(User::getId).collect(Collectors.toList());
            List<UserTrack> allTracks = userTrackMapper.findByUserIds(userIds);
            trackMap.putAll(allTracks.stream()
                    .collect(Collectors.groupingBy(
                            UserTrack::getUserId,
                            Collectors.mapping(UserTrack::getTrackId, Collectors.toList())
                    )));
            for (User u : users) {
                u.setTrackIds(trackMap.getOrDefault(u.getId(), Collections.emptyList()));
            }
        }

        // 过滤条件
        if (status != null) {
            users = users.stream().filter(u -> status.equals(u.getStatus())).collect(Collectors.toList());
        }
        if (keyword != null && !keyword.isEmpty()) {
            String[] kws = keyword.toLowerCase().split("\\s+");
            users = users.stream().filter(u -> {
                String username = u.getUsername() != null ? u.getUsername().toLowerCase() : "";
                String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                String wxName = u.getWxName() != null ? u.getWxName().toLowerCase() : "";
                for (String kw : kws) {
                    if (kw.isEmpty()) continue;
                    if (username.contains(kw) || email.contains(kw) || wxName.contains(kw)) {
                        return true;
                    }
                }
                return false;
            }).collect(Collectors.toList());
        }
        if (platform != null && !platform.isEmpty()) {
            users = users.stream().filter(u -> {
                String pl = u.getPlatformLimit();
                return pl != null && pl.contains(platform);
            }).collect(Collectors.toList());
        }
        if (trackId != null && !trackId.isEmpty()) {
            users = users.stream().filter(u -> {
                List<String> tids = trackMap.getOrDefault(u.getId(), Collections.emptyList());
                return tids.contains(trackId);
            }).collect(Collectors.toList());
        }
        if (userType != null) {
            users = users.stream().filter(u -> userType.equals(u.getUserType())).collect(Collectors.toList());
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
        // Auto-create order if newly created user is marked as account opened
        if (user.getUserType() != null && user.getUserType() == 1
                && user.getMembershipPlanId() != null) {
            MembershipPlan plan = membershipPlanService.getById(user.getMembershipPlanId());
            if (plan != null) {
                orderService.createAutoOrder(user, plan, "open_account");
            }
        }
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
        if (user.getWxName() != null) existing.setWxName(user.getWxName());
        if (user.getNickName() != null) existing.setNickName(user.getNickName());
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
        // Detect account opening: userType changed to 1 (开户)
        boolean wasOpened = existing.getUserType() != null && existing.getUserType() == 1;
        if (user.getUserType() != null) existing.setUserType(user.getUserType());
        boolean isNowOpened = existing.getUserType() != null && existing.getUserType() == 1;
        if (!wasOpened && isNowOpened) {
            MembershipPlan plan = membershipPlanService.getById(existing.getMembershipPlanId());
            if (plan != null) {
                orderService.createAutoOrder(existing, plan, "open_account");
            }
        }
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
            @RequestParam(required = false) Integer userType,
            @RequestParam(required = false) List<String> userIds) {
        try {
            List<User> users = userService.list();
            List<User> filtered = new ArrayList<>();
            for (User u : users) {
                if (userIds != null && !userIds.isEmpty() && !userIds.contains(u.getId())) continue;
                if (status != null && !status.equals(u.getStatus())) continue;
                if (userType != null && !userType.equals(u.getUserType())) continue;
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
            String[] cols = {"用户名", "手机号", "邮箱", "微信号", "公众号名称", "可选赛道数", "可访问平台", "到期时间", "会员套餐", "默认样式", "状态", "备注"};
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
                row.createCell(4).setCellValue(u.getWxName() != null ? u.getWxName() : "");
                row.createCell(5).setCellValue(u.getTrackLimit() != null ? u.getTrackLimit() : 0);
                row.createCell(6).setCellValue(u.getPlatformLimit() != null ? u.getPlatformLimit() : "");
                row.createCell(7).setCellValue(u.getExpireDate() != null ? u.getExpireDate().toString() : "");
                row.createCell(8).setCellValue(planMap.getOrDefault(u.getMembershipPlanId(), ""));
                row.createCell(9).setCellValue(u.getTemplate() != null ? u.getTemplate() : "");
                row.createCell(10).setCellValue(u.getStatus() != null && u.getStatus() == 1 ? "正常" : "已禁用");
                row.createCell(11).setCellValue(u.getRemark() != null ? u.getRemark() : "");
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
                String wxName = getCellString(row, 4);
                String trackLimitStr = getCellString(row, 5);
                String platformLimit = getCellString(row, 6);
                String expireDate = getCellString(row, 7);
                String membershipPlanName = getCellString(row, 8);
                String template = getCellString(row, 9);
                String statusStr = getCellString(row, 10);
                String remark = getCellString(row, 11);

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
                user.setWxName(wxName != null ? wxName : "");
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

    @GetMapping("/{userId}/next-title")
    public Result<List<Map<String, Object>>> getNextTitle(@PathVariable String userId, @RequestParam(value = "date", required = false) String date) {
        try {
            LocalDate recommendDate = (date != null && !date.isEmpty())
                    ? LocalDate.parse(date)
                    : LocalDate.now();
            List<Map<String, Object>> list = titleRecommendationMapper.findByUserAndDate(userId, recommendDate);
            // Deduplicate by trackId, keep the latest one
            Map<String, Map<String, Object>> deduped = new LinkedHashMap<>();
            for (Map<String, Object> item : list) {
                String trackId = (String) item.get("track_id");
                if (trackId == null) {
                    trackId = (String) item.get("trackId");
                }
                if (trackId != null && !deduped.containsKey(trackId)) {
                    deduped.put(trackId, item);
                }
            }
            return Result.ok(new ArrayList<>(deduped.values()));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/{userId}/next-title")
    public Result<List<Map<String, String>>> setNextTitle(@PathVariable String userId, @RequestBody Map<String, Object> body) {
        String recommendDateStr = (String) body.get("recommendDate");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> items = (List<Map<String, String>>) body.get("items");

        if (items == null || items.isEmpty()) {
            return Result.error("请至少输入一个标题");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        List<UserTrack> userTracks = userTrackMapper.findByUserId(userId);
        if (userTracks.isEmpty()) {
            return Result.error("该用户未订阅任何赛道");
        }
        Set<String> subscribedTrackIds = userTracks.stream()
                .map(UserTrack::getTrackId)
                .collect(Collectors.toSet());

        LocalDate recommendDate;
        try {
            recommendDate = (recommendDateStr != null && !recommendDateStr.isEmpty())
                    ? LocalDate.parse(recommendDateStr)
                    : LocalDate.now();
        } catch (Exception e) {
            return Result.error("推荐日期格式错误");
        }

        List<Map<String, String>> resultList = new ArrayList<>();
        for (Map<String, String> item : items) {
            String title = item.get("title");
            String trackId = item.get("trackId");
            if (title == null || title.trim().isEmpty()) {
                continue;
            }
            if (trackId == null || trackId.isEmpty() || !subscribedTrackIds.contains(trackId)) {
                continue;
            }
            Track track = trackMapper.findById(trackId);
            if (track == null) {
                continue;
            }

            TitleLibrary tl = new TitleLibrary();
            tl.setTitle(title.trim());
            tl.setPlatform(track.getPlatforms());
            tl.setTrackId(trackId);
            tl.setPushDate(recommendDate);
            tl.setRecommendUserId(userId);
            tl.setRecommendDate(recommendDate);
            titleLibraryService.save(tl);

            Map<String, String> resultItem = new HashMap<>();
            resultItem.put("titleLibraryId", tl.getId());
            resultItem.put("title", tl.getTitle());
            resultList.add(resultItem);
        }

        if (resultList.isEmpty()) {
            return Result.error("没有有效的标题被保存");
        }
        return Result.ok(resultList);
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
