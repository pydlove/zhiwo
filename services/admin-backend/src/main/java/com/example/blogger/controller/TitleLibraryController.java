package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.TitleLibrary;
import com.example.blogger.entity.TitleRecommendation;
import com.example.blogger.entity.Track;
import com.example.blogger.entity.User;
import com.example.blogger.entity.UserTrack;
import com.example.blogger.entity.*;
import com.example.blogger.mapper.*;
import com.example.blogger.service.EmailService;
import com.example.blogger.service.SubscriptionPostService;
import com.example.blogger.service.TitleLibraryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/title-library")
@CrossOrigin(origins = "*")
public class TitleLibraryController {

    private final TitleLibraryService titleLibraryService;
    private final TrackMapper trackMapper;
    private final UserMapper userMapper;
    private final UserTrackMapper userTrackMapper;
    private final TitleRecommendationMapper titleRecommendationMapper;
    private final DataSource dataSource;
    private final StyleMapper styleMapper;
    private final SubscriptionPostService subscriptionPostService;
    private final PromptTemplateMapper promptTemplateMapper;
    private final EmailService emailService;
    private final EmailPushLogMapper emailPushLogMapper;
    private final SubscriptionPostMapper subscriptionPostMapper;

    // Async generate task storage (for titles)
    private final ConcurrentHashMap<String, Map<String, Object>> generateTasks = new ConcurrentHashMap<>();
    // Async generate-post task storage (for subscription posts)
    private final ConcurrentHashMap<String, Map<String, Object>> generatePostTasks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public TitleLibraryController(TitleLibraryService titleLibraryService,
                                  TrackMapper trackMapper,
                                  UserMapper userMapper,
                                  UserTrackMapper userTrackMapper,
                                  TitleRecommendationMapper titleRecommendationMapper,
                                  DataSource dataSource,
                                  StyleMapper styleMapper,
                                  SubscriptionPostService subscriptionPostService,
                                  PromptTemplateMapper promptTemplateMapper,
                                  EmailService emailService,
                                  EmailPushLogMapper emailPushLogMapper,
                                  SubscriptionPostMapper subscriptionPostMapper) {
        this.titleLibraryService = titleLibraryService;
        this.trackMapper = trackMapper;
        this.userMapper = userMapper;
        this.userTrackMapper = userTrackMapper;
        this.titleRecommendationMapper = titleRecommendationMapper;
        this.dataSource = dataSource;
        this.styleMapper = styleMapper;
        this.subscriptionPostService = subscriptionPostService;
        this.promptTemplateMapper = promptTemplateMapper;
        this.emailService = emailService;
        this.emailPushLogMapper = emailPushLogMapper;
        this.subscriptionPostMapper = subscriptionPostMapper;
    }

    @PostConstruct
    public void migrateDescriptionColumn() {
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, null, "tu_title_library", "description")) {
            if (!rs.next()) {
                conn.createStatement().execute("ALTER TABLE tu_title_library ADD COLUMN description VARCHAR(500) COMMENT '标题SEO描述' AFTER title");
                System.out.println("Migration applied: added description column to tu_title_library");
            }
        } catch (SQLException e) {
            System.err.println("Migration check failed: " + e.getMessage());
        }
    }

    @PostConstruct
    public void migratePushDateColumn() {
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, null, "tu_title_library", "push_date")) {
            if (!rs.next()) {
                conn.createStatement().execute("ALTER TABLE tu_title_library ADD COLUMN push_date DATE NULL COMMENT '推荐日期' AFTER description");
                System.out.println("Migration applied: added push_date column to tu_title_library");
            }
        } catch (SQLException e) {
            System.err.println("Migration check failed for push_date: " + e.getMessage());
        }
    }

    @PostConstruct
    public void migrateSubscriptionPostIdColumn() {
        try (Connection conn = dataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM tu_title_recommendation LIKE 'subscription_post_id'")) {
            if (!rs.next()) {
                conn.createStatement().execute("ALTER TABLE tu_title_recommendation ADD COLUMN subscription_post_id VARCHAR(64) NULL COMMENT '关联的订阅文章ID'");
                conn.createStatement().execute("CREATE INDEX idx_title_recommendation_post_id ON tu_title_recommendation(subscription_post_id)");
                System.out.println("Migration applied: added subscription_post_id column to tu_title_recommendation");
            }
        } catch (SQLException e) {
            System.err.println("Migration check failed for subscription_post_id: " + e.getMessage());
        }
    }

    @PostConstruct
    public void migrateSubscriptionPostDescriptionColumn() {
        try (Connection conn = dataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM tu_subscription_post LIKE 'description'")) {
            if (rs.next()) {
                String type = rs.getString("Type");
                if (type != null && (type.toLowerCase().contains("varchar") || type.toLowerCase().contains("char"))) {
                    conn.createStatement().execute("ALTER TABLE tu_subscription_post MODIFY COLUMN description TEXT COMMENT '文章描述/内容'");
                    System.out.println("Migration applied: changed tu_subscription_post.description to TEXT");
                }
            }
        } catch (SQLException e) {
            System.err.println("Migration check failed for subscription_post.description: " + e.getMessage());
        }
    }

    @PostConstruct
    public void fixTrackLimitsOnStartup() {
        try (Connection conn = dataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT u.id, u.track_limit, p.track_limit as plan_track_limit " +
                 "FROM tu_user u " +
                 "JOIN tu_membership_plan p ON u.membership_plan_id = p.id " +
                 "WHERE u.membership_plan_id IS NOT NULL AND u.membership_plan_id != ''" +
                 "AND u.track_limit != p.track_limit")) {
            int fixed = 0;
            while (rs.next()) {
                String userId = rs.getString("id");
                int planLimit = rs.getInt("plan_track_limit");
                try (java.sql.PreparedStatement ps = conn.prepareStatement(
                        "UPDATE tu_user SET track_limit = ? WHERE id = ?")) {
                    ps.setInt(1, planLimit);
                    ps.setString(2, userId);
                    ps.executeUpdate();
                    fixed++;
                }
            }
            if (fixed > 0) {
                System.out.println("Fixed track_limit for " + fixed + " user(s) to match their current plan.");
            }
        } catch (SQLException e) {
            System.err.println("Fix track limits on startup failed: " + e.getMessage());
        }
    }

    @PostConstruct
    public void migratePromptTemplateTable() {
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, "tu_prompt_template", null)) {
            if (!rs.next()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS tu_prompt_template (" +
                    "  id VARCHAR(64) PRIMARY KEY," +
                    "  name VARCHAR(100) NOT NULL COMMENT '模板名称'," +
                    "  content TEXT NOT NULL COMMENT '提示词内容'," +
                    "  type VARCHAR(50) DEFAULT 'generate_post' COMMENT '类型'," +
                    "  is_default TINYINT DEFAULT 0 COMMENT '是否默认'," +
                    "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "  is_deleted TINYINT DEFAULT 0," +
                    "  INDEX idx_type (type)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提示词模板表'"
                );
                System.out.println("Migration applied: created tu_prompt_template table");
            }
        } catch (SQLException e) {
            System.err.println("Migration check failed for tu_prompt_template: " + e.getMessage());
        }
    }

    /**
     * 获取用户当前激活的赛道ID集合（按订阅时间先后，只保留前 trackLimit 个）
     */
    private Set<String> getActiveTrackIdsForUser(User user) {
        List<UserTrack> uts = userTrackMapper.findByUserId(user.getId());
        if (uts == null || uts.isEmpty()) {
            return Collections.emptySet();
        }
        int limit = user.getTrackLimit() != null ? user.getTrackLimit() : uts.size();
        if (limit <= 0) {
            return Collections.emptySet();
        }
        // findByUserId 已按 created_at ASC 排序，最早订阅的优先保留
        Set<String> active = new LinkedHashSet<>();
        for (int i = 0; i < Math.min(limit, uts.size()); i++) {
            active.add(uts.get(i).getTrackId());
        }
        return active;
    }

    @GetMapping("/debug-recommendations")
    public Result<Map<String, Object>> debugRecommendations() {
        try (Connection conn = dataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            Map<String, Object> result = new HashMap<>();

            // Count total recommendations
            ResultSet rs1 = stmt.executeQuery("SELECT COUNT(*) as cnt FROM tu_title_recommendation");
            if (rs1.next()) result.put("totalRecommendations", rs1.getInt("cnt"));

            // Count by subscription_post_id status
            ResultSet rs2 = stmt.executeQuery(
                "SELECT " +
                "  COUNT(CASE WHEN subscription_post_id IS NULL THEN 1 END) as null_count, " +
                "  COUNT(CASE WHEN subscription_post_id = '' THEN 1 END) as empty_count, " +
                "  COUNT(CASE WHEN subscription_post_id IS NOT NULL AND subscription_post_id != '' THEN 1 END) as has_post_count " +
                "FROM tu_title_recommendation");
            if (rs2.next()) {
                result.put("nullCount", rs2.getInt("null_count"));
                result.put("emptyCount", rs2.getInt("empty_count"));
                result.put("hasPostCount", rs2.getInt("has_post_count"));
            }

            // Show sample records without post
            ResultSet rs3 = stmt.executeQuery(
                "SELECT id, title_library_id, user_id, recommend_date, subscription_post_id, created_at " +
                "FROM tu_title_recommendation " +
                "WHERE subscription_post_id IS NULL OR subscription_post_id = '' " +
                "ORDER BY created_at DESC LIMIT 5");
            List<Map<String, Object>> samples = new ArrayList<>();
            while (rs3.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs3.getString("id"));
                row.put("titleLibraryId", rs3.getString("title_library_id"));
                row.put("userId", rs3.getString("user_id"));
                row.put("recommendDate", rs3.getString("recommend_date"));
                row.put("subscriptionPostId", rs3.getString("subscription_post_id"));
                row.put("createdAt", rs3.getString("created_at"));
                samples.add(row);
            }
            result.put("samplesWithoutPost", samples);

            return Result.ok(result);
        } catch (SQLException e) {
            return Result.error("Debug query failed: " + e.getMessage());
        }
    }

    @GetMapping
    public Result<?> list(
            @RequestParam(value = "platform", required = false) String platform,
            @RequestParam(value = "trackId", required = false) String trackId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "recommendUserName", required = false) String recommendUserName,
            @RequestParam(value = "matched", required = false) String matched,
            @RequestParam(value = "pushDate", required = false) String pushDate,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        boolean hasFilter = (platform != null && !platform.isEmpty())
                || (trackId != null && !trackId.isEmpty())
                || (keyword != null && !keyword.isEmpty())
                || (recommendUserName != null && !recommendUserName.isEmpty())
                || (matched != null && !matched.isEmpty())
                || (pushDate != null && !pushDate.isEmpty());
        if (page != null && pageSize != null && page > 0 && pageSize > 0) {
            if (hasFilter) {
                return Result.ok(titleLibraryService.searchPage(platform, trackId, keyword, recommendUserName, matched, pushDate, page, pageSize));
            }
            return Result.ok(titleLibraryService.listPage(page, pageSize));
        }
        if (hasFilter) {
            return Result.ok(titleLibraryService.search(platform, trackId, keyword, recommendUserName, matched, pushDate));
        }
        return Result.ok(titleLibraryService.list());
    }

    @PostMapping
    public Result<Void> save(@RequestBody TitleLibrary titleLibrary) {
        titleLibraryService.save(titleLibrary);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody TitleLibrary titleLibrary) {
        titleLibrary.setId(id);
        titleLibraryService.save(titleLibrary);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        titleLibraryService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importExcel(@RequestParam("excel") MultipartFile excel) {
        try {
            Workbook wb = new XSSFWorkbook(excel.getInputStream());
            List<String> errors = new ArrayList<>();
            int success = 0;
            int skip = 0;
            int updated = 0;
            int created = 0;

            for (int sheetIdx = 0; sheetIdx < wb.getNumberOfSheets(); sheetIdx++) {
                Sheet sheet = wb.getSheetAt(sheetIdx);
                if (sheet == null) continue;
                String sheetName = sheet.getSheetName();
                if ("生成规则".equals(sheetName)) continue; // skip rule sheet

                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;

                // Detect if first column is "ID"
                boolean hasIdColumn = false;
                Cell firstHeader = headerRow.getCell(0);
                if (firstHeader != null && firstHeader.getCellType() == CellType.STRING) {
                    String h = firstHeader.getStringCellValue();
                    if (h != null && (h.contains("ID") || h.contains("id"))) {
                        hasIdColumn = true;
                    }
                }

                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    int colOffset = hasIdColumn ? 1 : 0;
                    String id = hasIdColumn ? getCellString(row, 0) : null;
                    String title = getCellString(row, colOffset);
                    String description = getCellString(row, colOffset + 1);
                    String pushDateStr = getCellString(row, colOffset + 2);
                    String platform = getCellString(row, colOffset + 3);
                    String trackName = getCellString(row, colOffset + 4);
                    String recommendDateStr = getCellString(row, colOffset + 5);
                    String recommendUserName = getCellString(row, colOffset + 6);
                    String recommendUserTemplate = getCellString(row, colOffset + 7);

                    if (title == null || title.isEmpty()) {
                        skip++;
                        continue;
                    }

                    // 查找赛道
                    String trackId = null;
                    if (trackName != null && !trackName.isEmpty()) {
                        Track track = trackMapper.findByName(trackName);
                        if (track != null) {
                            trackId = track.getId();
                        } else {
                            errors.add("Sheet「" + sheetName + "」第" + (i + 1) + "行：赛道「" + trackName + "」不存在");
                        }
                    }

                    // 按用户名查找本地用户，恢复关联
                    String recommendUserId = null;
                    if (recommendUserName != null && !recommendUserName.isEmpty()) {
                        User localUser = userMapper.findByUsername(recommendUserName);
                        if (localUser != null) {
                            recommendUserId = localUser.getId();
                        }
                    }

                    TitleLibrary tl;
                    if (id != null && !id.isEmpty()) {
                        // Try to update existing record
                        tl = titleLibraryService.getById(id);
                        if (tl != null) {
                            tl.setTitle(title);
                            tl.setDescription(description);
                            tl.setPlatform(platform);
                            if (trackId != null) {
                                tl.setTrackId(trackId);
                            }
                            if (pushDateStr != null && !pushDateStr.isEmpty()) {
                                try { tl.setPushDate(java.time.LocalDate.parse(pushDateStr)); } catch (Exception ignored) {}
                            }
                            if (recommendUserId != null) {
                                tl.setRecommendUserId(recommendUserId);
                            }
                            if (recommendDateStr != null && !recommendDateStr.isEmpty()) {
                                try { tl.setRecommendDate(java.time.LocalDate.parse(recommendDateStr)); } catch (Exception ignored) {}
                            }
                            if (recommendUserTemplate != null && !recommendUserTemplate.isEmpty()) {
                                tl.setRecommendUserTemplate(recommendUserTemplate);
                            }
                            titleLibraryService.save(tl);
                            updated++;
                            success++;
                            continue;
                        }
                    }

                    tl = new TitleLibrary();
                    tl.setTitle(title);
                    tl.setDescription(description);
                    tl.setPlatform(platform);
                    tl.setTrackId(trackId);
                    tl.setUseCount(0);
                    if (pushDateStr != null && !pushDateStr.isEmpty()) {
                        try { tl.setPushDate(java.time.LocalDate.parse(pushDateStr)); } catch (Exception ignored) {}
                    }
                    if (recommendUserId != null) {
                        tl.setRecommendUserId(recommendUserId);
                    }
                    if (recommendDateStr != null && !recommendDateStr.isEmpty()) {
                        try { tl.setRecommendDate(java.time.LocalDate.parse(recommendDateStr)); } catch (Exception ignored) {}
                    }
                    if (recommendUserTemplate != null && !recommendUserTemplate.isEmpty()) {
                        tl.setRecommendUserTemplate(recommendUserTemplate);
                    }
                    titleLibraryService.save(tl);
                    created++;
                    success++;
                }
            }
            wb.close();

            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("created", created);
            result.put("updated", updated);
            result.put("skip", skip);
            result.put("errors", errors);
            return Result.ok(result);

        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @PostMapping("/export-titles")
    public void exportTitles(HttpServletResponse response,
            @RequestParam(value = "platform", required = false) String platform,
            @RequestParam(value = "trackId", required = false) String trackId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "recommendUserName", required = false) String recommendUserName,
            @RequestParam(value = "matched", required = false) String matched,
            @RequestParam(value = "pushDate", required = false) String pushDate,
            @RequestParam(value = "titleIds", required = false) List<String> titleIds) {
        try {
            List<TitleLibrary> titles;
            if (titleIds != null && !titleIds.isEmpty()) {
                titles = new ArrayList<>();
                for (String id : titleIds) {
                    TitleLibrary tl = titleLibraryService.getById(id);
                    if (tl != null) titles.add(tl);
                }
            } else {
                boolean hasFilter = (platform != null && !platform.isEmpty())
                        || (trackId != null && !trackId.isEmpty())
                        || (keyword != null && !keyword.isEmpty())
                        || (recommendUserName != null && !recommendUserName.isEmpty())
                        || (matched != null && !matched.isEmpty())
                        || (pushDate != null && !pushDate.isEmpty());
                titles = hasFilter
                        ? titleLibraryService.search(platform, trackId, keyword, recommendUserName, matched, pushDate)
                        : titleLibraryService.list();
            }

            XSSFWorkbook wb = new XSSFWorkbook();
            String[] headers = { "ID", "标题名称", "描述", "推送日期", "平台", "赛道", "推荐日期", "关联用户", "用户样式", "使用次数" };

            Sheet sheet = wb.createSheet("标题库");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            for (int i = 0; i < titles.size(); i++) {
                TitleLibrary tl = titles.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(tl.getId() != null ? tl.getId() : "");
                row.createCell(1).setCellValue(tl.getTitle() != null ? tl.getTitle() : "");
                row.createCell(2).setCellValue(tl.getDescription() != null ? tl.getDescription() : "");
                row.createCell(3).setCellValue(tl.getPushDate() != null ? tl.getPushDate().toString() : "");
                row.createCell(4).setCellValue(tl.getPlatform() != null ? tl.getPlatform() : "");
                row.createCell(5).setCellValue(tl.getTrackName() != null ? tl.getTrackName() : "");
                row.createCell(6).setCellValue(tl.getRecommendDate() != null ? tl.getRecommendDate().toString() : "");
                row.createCell(7).setCellValue(tl.getRecommendUserName() != null ? tl.getRecommendUserName() : "");
                row.createCell(8).setCellValue(tl.getRecommendUserTemplate() != null ? tl.getRecommendUserTemplate() : "");
                row.createCell(9).setCellValue(tl.getUseCount() != null ? tl.getUseCount() : 0);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }

            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "标题库_" + timestamp + ".xlsx";

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
            try (OutputStream out = response.getOutputStream()) {
                wb.write(out);
            }
            wb.close();

        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "导出失败：" + e.getMessage());
            } catch (IOException ignored) {}
        }
    }

    @PostMapping("/export")
    public void export(HttpServletResponse response,
            @RequestParam(value = "platform", required = false) String platform,
            @RequestParam(value = "trackId", required = false) String trackId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "recommendUserName", required = false) String recommendUserName,
            @RequestParam(value = "matched", required = false) String matched,
            @RequestParam(value = "pushDate", required = false) String pushDate,
            @RequestParam(value = "titleIds", required = false) List<String> titleIds) {
        try {
            List<TitleLibrary> titles;
            if (titleIds != null && !titleIds.isEmpty()) {
                titles = new ArrayList<>();
                for (String id : titleIds) {
                    TitleLibrary tl = titleLibraryService.getById(id);
                    if (tl != null) titles.add(tl);
                }
            } else {
                boolean hasFilter = (platform != null && !platform.isEmpty())
                        || (trackId != null && !trackId.isEmpty())
                        || (keyword != null && !keyword.isEmpty())
                        || (recommendUserName != null && !recommendUserName.isEmpty())
                        || (matched != null && !matched.isEmpty())
                        || (pushDate != null && !pushDate.isEmpty());
                titles = hasFilter
                        ? titleLibraryService.search(platform, trackId, keyword, recommendUserName, matched, pushDate)
                        : titleLibraryService.list();
            }

            // Group by platform-trackName
            Map<String, List<TitleLibrary>> grouped = new LinkedHashMap<>();
            for (TitleLibrary tl : titles) {
                String sheetName = tl.getPlatform() != null && !tl.getPlatform().isEmpty()
                        ? tl.getPlatform()
                        : "未分类";
                if (tl.getTrackName() != null && !tl.getTrackName().isEmpty()) {
                    sheetName = sheetName + "-" + tl.getTrackName();
                }
                grouped.computeIfAbsent(sheetName, k -> new ArrayList<>()).add(tl);
            }

            XSSFWorkbook wb = new XSSFWorkbook();
            String[] headers = { "ID", "标题名称", "用户名", "样式风格", "描述", "推荐日期", "是否创作完成" };

            for (Map.Entry<String, List<TitleLibrary>> entry : grouped.entrySet()) {
                String sheetName = entry.getKey();
                // Excel sheet name max 31 chars, avoid invalid chars
                if (sheetName.length() > 31) {
                    sheetName = sheetName.substring(0, 31);
                }
                sheetName = sheetName.replace(":", "-").replace("\\", "-").replace("/", "-").replace("?", "-").replace("*", "-").replace("[", "(").replace("]", ")");

                Sheet sheet = wb.createSheet(sheetName);
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }

                List<TitleLibrary> list = entry.getValue();
                for (int i = 0; i < list.size(); i++) {
                    TitleLibrary tl = list.get(i);
                    Row row = sheet.createRow(i + 1);
                    row.createCell(0).setCellValue(tl.getId() != null ? tl.getId() : "");
                    row.createCell(1).setCellValue(tl.getTitle() != null ? tl.getTitle() : "");
                    row.createCell(2).setCellValue(tl.getRecommendUserName() != null ? tl.getRecommendUserName() : "");
                    row.createCell(3).setCellValue(tl.getRecommendUserTemplate() != null ? tl.getRecommendUserTemplate() : "");
                    row.createCell(4).setCellValue(tl.getDescription() != null ? tl.getDescription() : "");
                    row.createCell(5).setCellValue(tl.getRecommendDate() != null ? tl.getRecommendDate().toString() : "");
                    boolean completed = tl.getSubscriptionPostTitle() != null && !tl.getSubscriptionPostTitle().isEmpty()
                            || tl.getSubscriptionPostFileUrl() != null && !tl.getSubscriptionPostFileUrl().isEmpty();
                    row.createCell(6).setCellValue(completed ? "是" : "");
                }

                for (int i = 0; i < headers.length; i++) {
                    sheet.setColumnWidth(i, 20 * 256);
                }
            }

            // Add "生成规则" sheet from database template
            String defaultRule = "以下是生成规则：\n" +
                "1、该sheet之前的每个sheet都是需要生成文章，在根目录下根据sheet名称创建文件夹，此文件夹作为该赛道的创作目录，根目录路径是/Users/panyong/aio_project/公众号/自媒体/;\n" +
                "2、创作规则：根据每个sheet里面的标题进行创作，并且你要根据你创作的内容生成一个120个字符以内的(包含标点符号)描述，描述要求符合SEO的原则，填写到\"描述\"列，\"创作日期\"列如果为空，就填写为为当天日期，如果不为空，就不用管，如果创作完成，\"是否创作完成\"填写是；\n" +
                "3、输出规则：输出的文件都是docx格式，并且根据每行数据的日期，在创作目录下创建一个输出目录，输出目录就是\"创作日期\"，文件输出到输出目录下，例如：/Users/panyong/aio_project/公众号/自媒体/职场/{date}；\n" +
                "4、输出文件样式：根据\"样式风格\"列的内容，去/Users/panyong/aio_project/小程序/services/admin-backend/styles目录下查找对应的样式文件，参考对应的样式输出文章；\n" +
                "5、文件内容图片生成规则：内容中要插入图片，你可以自行下载相关图片（下载一些和标题呼应的图片，可以多找一些资源，不要总是那么几张，下载的图片必须是16:9的）；\n" +
                "6、爆款标题生成原则：爆款标题要求通过标题就能吸引读者，也就是网上俗称的\"标题党\"，字数严禁超过30个字符；\n" +
                "7、对于\"是否创作完成\"填写\"是\"的数据，就不要再重复创作了，要创作\"是否创作完成\"为空或者\"否\"的数据；";

            com.example.blogger.entity.PromptTemplate ruleTemplate = promptTemplateMapper.findDefaultByType("export_rule");
            if (ruleTemplate == null) {
                ruleTemplate = promptTemplateMapper.findLatestByType("export_rule");
            }
            String ruleContent = (ruleTemplate != null && ruleTemplate.getContent() != null)
                    ? ruleTemplate.getContent() : defaultRule;

            Sheet ruleSheet = wb.createSheet("生成规则");
            Row ruleRow = ruleSheet.createRow(0);
            Cell ruleCell = ruleRow.createCell(0);
            ruleCell.setCellValue(ruleContent);
            ruleSheet.setColumnWidth(0, 60 * 256);

            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "标题库导出_" + timestamp + ".xlsx";

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
            try (OutputStream out = response.getOutputStream()) {
                wb.write(out);
            }
            wb.close();

        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "导出失败：" + e.getMessage());
            } catch (IOException ignored) {}
        }
    }

    @PostMapping("/import-articles")
    public Result<Map<String, Object>> importArticles(@RequestParam("files") MultipartFile[] files) {
        try {
            if (files == null || files.length == 0) {
                return Result.error("请选择文件");
            }

            // Load all titles for matching
            List<TitleLibrary> allTitles = titleLibraryService.list();

            String articlesDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "articles";
            File dir = new File(articlesDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            int success = 0;
            int skip = 0;
            List<Map<String, String>> errors = new ArrayList<>();
            List<Map<String, String>> details = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                String originalName = file.getOriginalFilename();
                if (originalName == null || originalName.isEmpty()) continue;

                // Extract filename without extension and path
                String baseName = originalName;
                int lastSlash = Math.max(baseName.lastIndexOf('/'), baseName.lastIndexOf('\\'));
                if (lastSlash >= 0) {
                    baseName = baseName.substring(lastSlash + 1);
                }

                // Only process .doc and .docx files
                String ext = "";
                int lastDot = baseName.lastIndexOf('.');
                if (lastDot > 0) {
                    ext = baseName.substring(lastDot + 1).toLowerCase();
                    baseName = baseName.substring(0, lastDot);
                }
                if (!"doc".equals(ext) && !"docx".equals(ext)) {
                    skip++;
                    continue;
                }

                // Clean filename: remove common prefixes like dates, numbers
                String cleanName = baseName.replaceAll("^\\d+[-_.\\s]*", "").trim().toLowerCase();
                if (cleanName.isEmpty()) {
                    cleanName = baseName.trim().toLowerCase();
                }

                // Find best matching TitleLibrary
                TitleLibrary matchedTitle = null;
                double bestScore = 0;
                for (TitleLibrary tl : allTitles) {
                    String title = tl.getTitle();
                    if (title == null || title.isEmpty()) continue;
                    String cleanTitle = title.trim().toLowerCase();

                    double score = calculateMatchScore(cleanName, cleanTitle);
                    if (score > bestScore) {
                        bestScore = score;
                        matchedTitle = tl;
                    }
                }

                if (matchedTitle == null || bestScore < 0.3) {
                    skip++;
                    errors.add(Map.of("file", originalName, "reason", "未找到匹配的标题记录（最佳相似度：" + String.format("%.2f", bestScore) + "）"));
                    continue;
                }

                // Find the latest TitleRecommendation for this title without a post
                TitleRecommendation rec = titleRecommendationMapper.findLatestByTitleId(matchedTitle.getId());
                if (rec == null) {
                    skip++;
                    errors.add(Map.of("file", originalName, "reason", "标题「" + matchedTitle.getTitle() + "」没有关联的推荐记录"));
                    continue;
                }

                // Save file with original name (use title + original extension to keep it readable)
                // Remove both ASCII and Chinese special chars that are invalid in filenames/URLs
                String safeBase = baseName.replaceAll("[\\\\/:*?\"<>|%\\uFF1A\\uFF1F\\u201C\\u201D\\u2018\\u2019\\u300A\\u300B\\u3001\\uFF0C\\u3002\\uFF01\\uFF08\\uFF09]", "_").trim();
                if (safeBase.isEmpty()) {
                    safeBase = matchedTitle.getTitle() != null ? matchedTitle.getTitle().replaceAll("[\\\\/:*?\"<>|%\\uFF1A\\uFF1F\\u201C\\u201D\\u2018\\u2019\\u300A\\u300B\\u3001\\uFF0C\\u3002\\uFF01\\uFF08\\uFF09]", "_").trim() : "article";
                }
                String fileName = safeBase + "." + ext;
                // Avoid overwrite: if exists, append counter
                File targetFile = new File(articlesDir + File.separator + fileName);
                int counter = 1;
                while (targetFile.exists()) {
                    fileName = safeBase + "(" + counter + ")." + ext;
                    targetFile = new File(articlesDir + File.separator + fileName);
                    counter++;
                }
                String filePath = articlesDir + File.separator + fileName;
                file.transferTo(new File(filePath));

                // Create or update SubscriptionPost
                String fileUrl = "/uploads/articles/" + fileName;
                SubscriptionPost post;
                if (rec.getSubscriptionPostId() != null && !rec.getSubscriptionPostId().isEmpty()) {
                    post = subscriptionPostService.getById(rec.getSubscriptionPostId());
                    if (post == null) {
                        post = new SubscriptionPost();
                    }
                } else {
                    post = new SubscriptionPost();
                }
                post.setUserId(rec.getUserId());
                post.setTrackId(rec.getTrackId());
                post.setTitle(matchedTitle.getTitle());
                post.setDescription("");
                post.setFileUrl(fileUrl);
                post.setFileName(fileName);
                post.setStatus("已上架");
                post.setUsed(0);
                subscriptionPostService.save(post);

                // Update recommendation
                titleRecommendationMapper.updateSubscriptionPostId(rec.getId(), post.getId());

                success++;
                details.add(Map.of(
                    "file", originalName,
                    "title", matchedTitle.getTitle(),
                    "user", rec.getUserName() != null ? rec.getUserName() : ""
                ));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("skip", skip);
            result.put("errors", errors);
            result.put("details", details);
            return Result.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    /**
     * Calculate match score between filename and title.
     * Returns value between 0 and 1, higher is better match.
     */
    private double calculateMatchScore(String name, String title) {
        if (name.equals(title)) return 1.0;
        if (name.contains(title) || title.contains(name)) {
            // Bidirectional containment - good match
            double ratio = (double) Math.min(name.length(), title.length()) / Math.max(name.length(), title.length());
            return 0.6 + 0.4 * ratio;
        }
        // Levenshtein distance similarity
        int distance = levenshteinDistance(name, title);
        int maxLen = Math.max(name.length(), title.length());
        if (maxLen == 0) return 0;
        return 1.0 - (double) distance / maxLen;
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    @PostMapping("/match-today")
    public Result<Map<String, Object>> matchToday() {
        try {
            LocalDate today = LocalDate.now();
            List<TitleLibrary> allTitles = titleLibraryService.list();
            // Only match titles whose pushDate is today
            List<TitleLibrary> titles = allTitles.stream()
                    .filter(t -> t.getPushDate() != null && t.getPushDate().equals(today))
                    .collect(Collectors.toList());
            List<User> users = userMapper.findAll();

            // Build user active track subscriptions map (only non-frozen tracks)
            Map<String, Set<String>> userTrackMap = new HashMap<>();
            for (User user : users) {
                Set<String> activeTrackIds = getActiveTrackIdsForUser(user);
                userTrackMap.put(user.getId(), activeTrackIds);
            }

            int matched = 0;
            int skipped = 0;
            Random random = new Random();

            for (TitleLibrary title : titles) {
                // Find eligible users
                List<User> eligible = new ArrayList<>();
                for (User user : users) {
                    // Check platform match
                    if (title.getPlatform() != null && !title.getPlatform().isEmpty()) {
                        String platformLimit = user.getPlatformLimit();
                        if (platformLimit == null || platformLimit.isEmpty()) continue;
                        Set<String> userPlatforms = new HashSet<>(Arrays.asList(platformLimit.split(",")));
                        if (!userPlatforms.contains(title.getPlatform())) continue;
                    }

                    // Check track match
                    if (title.getTrackId() != null && !title.getTrackId().isEmpty()) {
                        Set<String> userTracks = userTrackMap.getOrDefault(user.getId(), Collections.emptySet());
                        if (!userTracks.contains(title.getTrackId())) continue;
                    }

                    // Check if this user already has a recommendation for this platform + track today
                    int existing = titleRecommendationMapper.countByUserPlatformTrackDate(
                            user.getId(), title.getPlatform(), title.getTrackId(), today);
                    if (existing > 0) continue;

                    eligible.add(user);
                }

                if (eligible.isEmpty()) {
                    skipped++;
                    continue;
                }

                // Randomly select one user
                User selected = eligible.get(random.nextInt(eligible.size()));

                TitleRecommendation rec = new TitleRecommendation();
                rec.setId(UUID.randomUUID().toString().replace("-", ""));
                rec.setTitleLibraryId(title.getId());
                rec.setUserId(selected.getId());
                rec.setPlatform(title.getPlatform());
                rec.setTrackId(title.getTrackId());
                rec.setRecommendDate(today);
                titleRecommendationMapper.insert(rec);

                matched++;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("matched", matched);
            result.put("skipped", skipped);
            return Result.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("匹配失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/recommendation")
    public Result<Void> unbindRecommendation(@PathVariable String id) {
        titleRecommendationMapper.deleteByTitleId(id);
        return Result.ok(null);
    }

    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestBody Map<String, Object> params) {
        int countPerCombo = params.get("countPerCombo") != null ?
                Integer.parseInt(params.get("countPerCombo").toString()) : 3;
        String rawOutputPath = params.get("outputPath") != null ?
                params.get("outputPath").toString() : "";

        final String outputPath;
        if (rawOutputPath.isEmpty()) {
            File projectRoot = new File(System.getProperty("user.dir"));
            if (projectRoot.getParentFile() != null && projectRoot.getParentFile().getParentFile() != null) {
                projectRoot = projectRoot.getParentFile().getParentFile();
            }
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            outputPath = projectRoot.getAbsolutePath() + File.separator + "export" + File.separator + "生成标题_" + timestamp + ".xlsx";
        } else {
            outputPath = rawOutputPath;
        }

        @SuppressWarnings("unchecked")
        List<String> selectedPlatforms = (List<String>) params.get("platforms");
        @SuppressWarnings("unchecked")
        List<String> selectedTrackIds = (List<String>) params.get("trackIds");

        String taskId = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> task = new ConcurrentHashMap<>();
        task.put("status", "running");
        task.put("progress", 0);
        task.put("message", "任务已提交，正在准备生成...");
        task.put("total", 0);
        task.put("path", outputPath);
        generateTasks.put(taskId, task);

        executor.submit(() -> runGenerateTask(taskId, countPerCombo, outputPath, selectedPlatforms, selectedTrackIds));

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        return Result.ok(result);
    }

    @GetMapping("/generate-status")
    public Result<Map<String, Object>> generateStatus(@RequestParam("taskId") String taskId) {
        Map<String, Object> task = generateTasks.get(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }
        return Result.ok(new HashMap<>(task));
    }

    @PostMapping("/generate-cancel")
    public Result<Void> generateCancel(@RequestParam("taskId") String taskId) {
        Map<String, Object> task = generateTasks.get(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }
        String status = (String) task.get("status");
        if ("running".equals(status)) {
            task.put("status", "cancelled");
            task.put("message", "任务已取消");
        }
        return Result.ok(null);
    }

    // ---------------- Generate Posts for Today's Recommendations ----------------

    @PostMapping("/generate-posts-for-today")
    public Result<Map<String, Object>> generatePostsForToday(@RequestBody(required = false) Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> titleIds = params != null ? (List<String>) params.get("titleIds") : null;

        String taskId = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> task = new ConcurrentHashMap<>();
        task.put("status", "running");
        task.put("progress", 0);
        task.put("message", "任务已提交，正在准备生成文章...");
        task.put("total", 0);
        task.put("completed", 0);
        generatePostTasks.put(taskId, task);

        executor.submit(() -> runGeneratePostTask(taskId, titleIds));

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        return Result.ok(result);
    }

    @GetMapping("/generate-post-status")
    public Result<Map<String, Object>> generatePostStatus(@RequestParam("taskId") String taskId) {
        Map<String, Object> task = generatePostTasks.get(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }
        return Result.ok(new HashMap<>(task));
    }

    @PostMapping("/generate-post-cancel")
    public Result<Void> generatePostCancel(@RequestParam("taskId") String taskId) {
        Map<String, Object> task = generatePostTasks.get(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }
        String status = (String) task.get("status");
        if ("running".equals(status)) {
            task.put("status", "cancelled");
            task.put("message", "任务已取消");
        }
        return Result.ok(null);
    }

    @PostMapping("/{id}/send-email")
    public Result<Map<String, Object>> sendEmail(@PathVariable String id) {
        try {
            TitleLibrary titleLib = titleLibraryService.getById(id);
            if (titleLib == null) {
                return Result.error("标题不存在");
            }

            // Find the latest recommendation for this title
            TitleRecommendation rec = titleRecommendationMapper.findLatestByTitleId(id);
            if (rec == null) {
                return Result.error("该标题尚未关联用户，无法发送邮件");
            }

            if (rec.getSubscriptionPostId() == null || rec.getSubscriptionPostId().isEmpty()) {
                return Result.error("该标题尚未生成文章，无法发送邮件");
            }

            SubscriptionPost post = subscriptionPostService.getById(rec.getSubscriptionPostId());
            if (post == null) {
                return Result.error("关联的文章不存在");
            }

            User user = userMapper.findById(rec.getUserId());
            if (user == null) {
                return Result.error("关联的用户不存在");
            }

            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                return Result.error("该用户未设置邮箱地址");
            }

            String fileUrl = post.getFileUrl();
            if (fileUrl == null || fileUrl.isEmpty()) {
                return Result.error("文章文件路径为空");
            }

            String filePath = fileUrl.startsWith("/")
                    ? System.getProperty("user.dir") + fileUrl
                    : fileUrl;
            File articleFile = new File(filePath);
            if (!articleFile.exists()) {
                // Try resolving from uploads directory
                String articlesDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "articles";
                articleFile = new File(articlesDir + File.separator + post.getFileName());
            }
            if (!articleFile.exists()) {
                return Result.error("文章文件不存在，请重新生成");
            }

            Track userTrack = trackMapper.findById(rec.getTrackId());
            String trackName = userTrack != null ? userTrack.getName() : "";

            emailService.sendDailyRecommendEmail(
                    user.getEmail(),
                    user.getUsername(),
                    trackName,
                    titleLib.getTitle(),
                    titleLib.getPlatform(),
                    articleFile,
                    post.getFileName()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("userName", user.getUsername());
            result.put("email", user.getEmail());
            result.put("title", titleLib.getTitle());
            return Result.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("邮件发送失败：" + e.getMessage());
        }
    }

    @PostMapping("/batch-send-email")
    public Result<Map<String, Object>> batchSendEmail(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<String> titleIds = (List<String>) body.get("titleIds");
            if (titleIds == null || titleIds.isEmpty()) {
                return Result.error("请选择要发送邮件的标题");
            }

            int total = titleIds.size();
            int success = 0;
            int failed = 0;
            List<Map<String, String>> errors = new ArrayList<>();

            for (String titleId : titleIds) {
                try {
                    TitleLibrary titleLib = titleLibraryService.getById(titleId);
                    if (titleLib == null) {
                        failed++;
                        errors.add(Map.of("titleId", titleId, "reason", "标题不存在"));
                        continue;
                    }

                    TitleRecommendation rec = titleRecommendationMapper.findLatestByTitleId(titleId);
                    if (rec == null) {
                        failed++;
                        errors.add(Map.of("title", titleLib.getTitle(), "reason", "尚未关联用户"));
                        continue;
                    }

                    if (rec.getSubscriptionPostId() == null || rec.getSubscriptionPostId().isEmpty()) {
                        failed++;
                        errors.add(Map.of("title", titleLib.getTitle(), "reason", "尚未生成文章"));
                        continue;
                    }

                    SubscriptionPost post = subscriptionPostService.getById(rec.getSubscriptionPostId());
                    if (post == null) {
                        failed++;
                        errors.add(Map.of("title", titleLib.getTitle(), "reason", "关联文章不存在"));
                        continue;
                    }

                    User user = userMapper.findById(rec.getUserId());
                    if (user == null) {
                        failed++;
                        errors.add(Map.of("title", titleLib.getTitle(), "reason", "关联用户不存在"));
                        continue;
                    }

                    if (user.getEmail() == null || user.getEmail().isEmpty()) {
                        failed++;
                        errors.add(Map.of("title", titleLib.getTitle(), "reason", "用户未设置邮箱"));
                        continue;
                    }

                    String fileUrl = post.getFileUrl();
                    if (fileUrl == null || fileUrl.isEmpty()) {
                        failed++;
                        errors.add(Map.of("title", titleLib.getTitle(), "reason", "文章文件路径为空"));
                        continue;
                    }

                    String filePath = fileUrl.startsWith("/")
                            ? System.getProperty("user.dir") + fileUrl
                            : fileUrl;
                    File articleFile = new File(filePath);
                    if (!articleFile.exists()) {
                        String articlesDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "articles";
                        articleFile = new File(articlesDir + File.separator + post.getFileName());
                    }
                    if (!articleFile.exists()) {
                        failed++;
                        errors.add(Map.of("title", titleLib.getTitle(), "reason", "文章文件不存在"));
                        continue;
                    }

                    Track userTrack = trackMapper.findById(rec.getTrackId());
                    String trackName = userTrack != null ? userTrack.getName() : "";

                    emailService.sendDailyRecommendEmail(
                            user.getEmail(),
                            user.getUsername(),
                            trackName,
                            titleLib.getTitle(),
                            titleLib.getPlatform(),
                            articleFile,
                            post.getFileName()
                    );

                    success++;
                } catch (Exception e) {
                    failed++;
                    errors.add(Map.of("titleId", titleId, "reason", e.getMessage()));
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total", total);
            result.put("success", success);
            result.put("failed", failed);
            result.put("errors", errors);
            return Result.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("批量发送失败：" + e.getMessage());
        }
    }

    // ========== 右侧面板：未推荐用户 / 未推送用户 ==========

    @GetMapping("/unrecommended-users")
    public Result<List<Map<String, Object>>> listUnrecommendedUsers(@RequestParam String date) {
        try {
            LocalDate pushDate = LocalDate.parse(date);
            List<Map<String, Object>> users = titleLibraryService.findUnrecommendedUsers(pushDate);
            return Result.ok(users);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/unpushed-users")
    public Result<List<Map<String, Object>>> listUnpushedUsers(@RequestParam String date) {
        try {
            LocalDate pushDate = LocalDate.parse(date);
            List<Map<String, Object>> users = titleLibraryService.findUnpushedUsers(pushDate);
            return Result.ok(users);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/batch-push-email")
    public Result<Map<String, Object>> batchPushEmail(@RequestBody Map<String, Object> body) {
        try {
            String dateStr = (String) body.get("date");
            @SuppressWarnings("unchecked")
            List<String> userIds = (List<String>) body.get("userIds");
            if (dateStr == null || dateStr.isEmpty()) {
                return Result.error("日期不能为空");
            }
            if (userIds == null || userIds.isEmpty()) {
                return Result.error("请选择要推送的用户");
            }

            LocalDate pushDate = LocalDate.parse(dateStr);
            int total = userIds.size();
            int success = 0;
            int failed = 0;
            List<Map<String, String>> errors = new ArrayList<>();

            for (String userId : userIds) {
                try {
                    User user = userMapper.findById(userId);
                    if (user == null) {
                        failed++;
                        errors.add(Map.of("userId", userId, "reason", "用户不存在"));
                        continue;
                    }
                    if (user.getStatus() == null || user.getStatus() != 1) {
                        failed++;
                        errors.add(Map.of("user", user.getUsername(), "reason", "用户已禁用"));
                        continue;
                    }
                    if (user.getEmail() == null || user.getEmail().isEmpty()) {
                        failed++;
                        errors.add(Map.of("user", user.getUsername(), "reason", "用户未设置邮箱"));
                        continue;
                    }

                    // 查找用户当日最新推荐（有关联文章的）
                    TitleRecommendation rec = titleRecommendationMapper.findLatestByUserAndDate(userId, pushDate);
                    if (rec == null || rec.getSubscriptionPostId() == null || rec.getSubscriptionPostId().isEmpty()) {
                        failed++;
                        errors.add(Map.of("user", user.getUsername(), "reason", "当日没有关联文章的推荐"));
                        continue;
                    }

                    SubscriptionPost post = subscriptionPostMapper.findById(rec.getSubscriptionPostId());
                    if (post == null) {
                        failed++;
                        errors.add(Map.of("user", user.getUsername(), "reason", "关联文章不存在"));
                        continue;
                    }

                    String fileUrl = post.getFileUrl();
                    if (fileUrl == null || fileUrl.isEmpty()) {
                        failed++;
                        errors.add(Map.of("user", user.getUsername(), "reason", "文章文件路径为空"));
                        continue;
                    }

                    String filePath = fileUrl.startsWith("/")
                            ? System.getProperty("user.dir") + fileUrl
                            : fileUrl;
                    File articleFile = new File(filePath);
                    if (!articleFile.exists()) {
                        String articlesDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "articles";
                        articleFile = new File(articlesDir + File.separator + post.getFileName());
                    }
                    if (!articleFile.exists()) {
                        failed++;
                        errors.add(Map.of("user", user.getUsername(), "reason", "文章文件不存在"));
                        continue;
                    }

                    Track userTrack = trackMapper.findById(rec.getTrackId());
                    String trackName = userTrack != null ? userTrack.getName() : "";

                    TitleLibrary titleLib = titleLibraryService.getById(rec.getTitleLibraryId());
                    String articleTitle = titleLib != null ? titleLib.getTitle() : post.getTitle();
                    String platform = titleLib != null && titleLib.getPlatform() != null ? titleLib.getPlatform() : "";

                    emailService.sendDailyRecommendEmail(
                            user.getEmail(),
                            user.getUsername(),
                            trackName,
                            articleTitle,
                            platform,
                            articleFile,
                            post.getFileName()
                    );

                    // 记录推送日志
                    EmailPushLog log = new EmailPushLog();
                    log.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
                    log.setUserId(userId);
                    log.setPushDate(pushDate);
                    log.setType("daily_recommend");
                    log.setTitleLibraryId(rec.getTitleLibraryId());
                    emailPushLogMapper.insert(log);

                    success++;
                } catch (Exception e) {
                    failed++;
                    errors.add(Map.of("userId", userId, "reason", e.getMessage()));
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total", total);
            result.put("success", success);
            result.put("failed", failed);
            result.put("errors", errors);
            return Result.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("批量推送失败：" + e.getMessage());
        }
    }

    private void runGeneratePostTask(String taskId, List<String> titleIds) {
        Map<String, Object> task = generatePostTasks.get(taskId);
        try {
            LocalDate today = LocalDate.now();
            List<TitleRecommendation> recommendations = titleRecommendationMapper.findTodayRecommendationsWithoutPost(today);
            if (titleIds != null && !titleIds.isEmpty()) {
                recommendations = recommendations.stream()
                        .filter(r -> titleIds.contains(r.getTitleLibraryId()))
                        .collect(Collectors.toList());
            }
            if (recommendations == null || recommendations.isEmpty()) {
                task.put("status", "completed");
                task.put("progress", 100);
                task.put("total", 0);
                task.put("completed", 0);
                task.put("message", "没有需要生成文章的数据");
                return;
            }

            task.put("total", recommendations.size());
            int completed = 0;
            int successCount = 0;
            int failCount = 0;

            for (TitleRecommendation rec : recommendations) {
                // Check cancellation
                Map<String, Object> currentTask = generatePostTasks.get(taskId);
                if (currentTask != null && "cancelled".equals(currentTask.get("status"))) {
                    task.put("status", "cancelled");
                    task.put("message", "任务已取消，已生成 " + successCount + " 篇，失败 " + failCount + " 篇");
                    return;
                }

                TitleLibrary titleLib = titleLibraryService.getById(rec.getTitleLibraryId());
                if (titleLib == null) {
                    failCount++;
                    continue;
                }

                User user = userMapper.findById(rec.getUserId());
                if (user == null) {
                    failCount++;
                    continue;
                }

                // Get user's style
                Style style = null;
                if (user.getTemplate() != null && !user.getTemplate().isEmpty()) {
                    style = styleMapper.findByName(user.getTemplate());
                }
                if (style == null) {
                    style = styleMapper.findDefault();
                }

                // Parse style JSON
                String styleDesc = "";
                String styleCss = buildDefaultStyleCss();
                if (style != null && style.getStyleJson() != null && !style.getStyleJson().isEmpty()) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode styleNode = mapper.readTree(style.getStyleJson());
                        StringBuilder sb = new StringBuilder();
                        sb.append("字体：").append(styleNode.path("fontFamily").asText("默认")).append("；");
                        sb.append("正文字号：").append(styleNode.path("fontSize").asText("16px")).append("；");
                        sb.append("行高：").append(styleNode.path("lineHeight").asText("1.8")).append("；");
                        sb.append("段落间距：").append(styleNode.path("paragraphSpacing").asText("1em")).append("；");
                        sb.append("标题颜色：").append(styleNode.path("titleColor").asText("#333")).append("；");
                        sb.append("正文颜色：").append(styleNode.path("textColor").asText("#333")).append("；");
                        sb.append("引用背景：").append(styleNode.path("quoteBg").asText("#f5f5f5")).append("；");
                        sb.append("H1字号：").append(styleNode.path("h1Size").asText("24px")).append("；");
                        sb.append("H2字号：").append(styleNode.path("h2Size").asText("20px"));
                        styleDesc = sb.toString();
                        styleCss = buildStyleCss(styleNode);
                    } catch (Exception e) {
                        // ignore parse error, use defaults
                    }
                }

                task.put("message", "正在生成文章（" + (completed + 1) + "/" + recommendations.size() + "）：" + titleLib.getTitle());

                // Build style reference prompt and determine working directory
                StringBuilder styleRefPrompt = new StringBuilder();
                File claudeWorkingDir = new File(System.getProperty("user.home"));
                boolean hasStyleRef = false;

                // Check exported styles directory first, then fallback to original directory
                String[] possibleStyleDirs = {
                    System.getProperty("user.dir") + File.separator + "styles",
                    "/Users/panyong/aio_project/公众号/样式"
                };

                if (style != null && style.getName() != null && !style.getName().isEmpty()) {
                    String docxName = style.getName() + ".docx";
                    for (String dirPath : possibleStyleDirs) {
                        File f = new File(dirPath, docxName);
                        if (f.exists()) {
                            styleRefPrompt.append("请严格参考 `@").append(docxName).append("` 文件的排版风格、配色方案、字体搭配和整体视觉调性来生成文章。\n\n");
                            claudeWorkingDir = new File(dirPath);
                            hasStyleRef = true;
                            break;
                        }
                    }
                }

                if (!hasStyleRef && rec.getTrackId() != null && !rec.getTrackId().isEmpty()) {
                    Track track = trackMapper.findById(rec.getTrackId());
                    if (track != null && track.getName() != null && !track.getName().isEmpty()) {
                        String trackDir = "/Users/panyong/aio_project/公众号/" + track.getName();
                        File trackDirFile = new File(trackDir);
                        if (trackDirFile.exists() && trackDirFile.isDirectory()) {
                            styleRefPrompt.append("请参考 `/Users/panyong/aio_project/公众号/").append(track.getName()).append("` 目录下其他文章的排版风格、配色方案和整体视觉调性来生成文章。\n\n");
                            claudeWorkingDir = trackDirFile;
                            hasStyleRef = true;
                        }
                    }
                }

                // Load custom prompt template
                com.example.blogger.entity.PromptTemplate promptTemplate = promptTemplateMapper.findDefaultByType("generate_post");
                if (promptTemplate == null) {
                    promptTemplate = promptTemplateMapper.findLatestByType("generate_post");
                }

                String promptText;
                if (promptTemplate != null && promptTemplate.getContent() != null && !promptTemplate.getContent().isEmpty()) {
                    // Use custom prompt template with variable substitution
                    promptText = promptTemplate.getContent()
                            .replace("{title}", titleLib.getTitle() != null ? titleLib.getTitle() : "")
                            .replace("{description}", titleLib.getDescription() != null ? titleLib.getDescription() : "")
                            .replace("{styleDesc}", styleDesc)
                            .replace("{styleRef}", styleRefPrompt.toString());
                } else {
                    // Fallback to default hard-coded prompt
                    StringBuilder prompt = new StringBuilder();
                    prompt.append("请根据以下标题和描述，生成一篇完整的公众号风格文章。\n\n");
                    prompt.append("标题：").append(titleLib.getTitle()).append("\n");
                    prompt.append("描述：").append(titleLib.getDescription() != null ? titleLib.getDescription() : "").append("\n\n");
                    if (!styleDesc.isEmpty()) {
                        prompt.append("文章的视觉样式要求如下（生成内容时必须在视觉上体现这些样式特征）：\n");
                        prompt.append(styleDesc).append("\n\n");
                    }
                    if (styleRefPrompt.length() > 0) {
                        prompt.append(styleRefPrompt);
                    }
                    prompt.append("要求：\n");
                    prompt.append("1. 文章必须围绕标题主题展开，内容充实、有深度、有观点\n");
                    prompt.append("2. 文章结构清晰，包含开头引入、正文论述、结尾总结\n");
                    prompt.append("3. 适合公众号传播，语言自然流畅，有阅读吸引力\n");
                    prompt.append("4. 文章长度适中，约800-1500字\n");
                    prompt.append("5. 输出纯HTML正文内容（不含html/head/body标签，只返回div包裹的内容），使用h1/h2/p/blockquote等标签组织内容\n");
                    prompt.append("6. 不要在任何标签上添加 style 属性或 class 属性，保持标签纯净\n");
                    prompt.append("7. 只输出纯JSON，不要markdown代码块，不要任何额外文字，不要在JSON前添加任何说明\n\n");
                    prompt.append("格式：{\"content\":\"<div>文章HTML内容</div>\"}");
                    promptText = prompt.toString();
                }

                String content = callClaudeForContent(promptText, claudeWorkingDir);
                if (content == null || content.isEmpty()) {
                    failCount++;
                    completed++;
                    task.put("completed", completed);
                    task.put("progress", recommendations.size() > 0 ? (completed * 100 / recommendations.size()) : 0);
                    continue;
                }

                // Wrap with style CSS
                String fullHtml = wrapContentWithStyle(titleLib.getTitle(), content, styleCss);

                // Save HTML file
                String articlesDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "articles";
                File dir = new File(articlesDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String fileName = "article_" + rec.getId() + "_" + System.currentTimeMillis() + ".html";
                String filePath = articlesDir + File.separator + fileName;
                try (FileWriter fw = new FileWriter(filePath, StandardCharsets.UTF_8)) {
                    fw.write(fullHtml);
                }

                // Create subscription post
                SubscriptionPost post = new SubscriptionPost();
                post.setUserId(rec.getUserId());
                post.setTrackId(rec.getTrackId());
                post.setTitle(titleLib.getTitle());
                post.setDescription(content);
                post.setFileUrl("/uploads/articles/" + fileName);
                post.setFileName(fileName);
                post.setStatus("已上架");
                post.setUsed(0);
                subscriptionPostService.save(post);

                // Update recommendation with subscription_post_id
                titleRecommendationMapper.updateSubscriptionPostId(rec.getId(), post.getId());

                successCount++;
                completed++;
                task.put("completed", completed);
                task.put("progress", recommendations.size() > 0 ? (completed * 100 / recommendations.size()) : 0);
            }

            task.put("status", "completed");
            task.put("progress", 100);
            task.put("message", "生成完成，成功 " + successCount + " 篇，失败 " + failCount + " 篇");

        } catch (Exception e) {
            e.printStackTrace();
            task.put("status", "failed");
            task.put("message", "生成失败：" + e.getMessage());
        }
    }

    private String buildDefaultStyleCss() {
        return ".article-content{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif !important;line-height:1.8 !important;color:#333 !important;}"
                + ".article-content h1{font-size:24px !important;font-weight:700 !important;color:#1a1a1a !important;margin-bottom:16px !important;line-height:1.4 !important;}"
                + ".article-content h2{font-size:20px !important;font-weight:600 !important;color:#1a1a1a !important;margin-top:24px !important;margin-bottom:12px !important;line-height:1.4 !important;}"
                + ".article-content p{font-size:16px !important;margin-bottom:16px !important;text-align:justify !important;line-height:1.8 !important;color:#333 !important;}"
                + ".article-content blockquote{background:#f5f5f5 !important;border-left:4px solid #1890ff !important;padding:12px 16px !important;margin:16px 0 !important;color:#555 !important;}"
                + "body{max-width:680px;margin:0 auto;padding:24px;}"
                + ".article-title{font-size:24px;font-weight:700;color:#1a1a1a;margin-bottom:20px;line-height:1.4;}";
    }

    private String buildStyleCss(JsonNode styleNode) {
        String fontFamily = styleNode.path("fontFamily").asText("-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif");
        String fontSize = styleNode.path("fontSize").asText("16px");
        String lineHeight = styleNode.path("lineHeight").asText("1.8");
        String paragraphSpacing = styleNode.path("paragraphSpacing").asText("1em");
        String titleColor = styleNode.path("titleColor").asText("#1a1a1a");
        String textColor = styleNode.path("textColor").asText("#333");
        String quoteBg = styleNode.path("quoteBg").asText("#f5f5f5");
        String h1Size = styleNode.path("h1Size").asText("24px");
        String h2Size = styleNode.path("h2Size").asText("20px");

        return ".article-content{font-family:" + fontFamily + " !important;line-height:" + lineHeight + " !important;color:" + textColor + " !important;}"
                + ".article-content h1{font-size:" + h1Size + " !important;font-weight:700 !important;color:" + titleColor + " !important;margin-bottom:" + paragraphSpacing + " !important;line-height:1.4 !important;}"
                + ".article-content h2{font-size:" + h2Size + " !important;font-weight:600 !important;color:" + titleColor + " !important;margin-top:24px !important;margin-bottom:12px !important;line-height:1.4 !important;}"
                + ".article-content p{font-size:" + fontSize + " !important;margin-bottom:" + paragraphSpacing + " !important;text-align:justify !important;line-height:" + lineHeight + " !important;color:" + textColor + " !important;}"
                + ".article-content blockquote{background:" + quoteBg + " !important;border-left:4px solid #1890ff !important;padding:12px 16px !important;margin:16px 0 !important;color:#555 !important;}"
                + "body{max-width:680px;margin:0 auto;padding:24px;}"
                + ".article-title{font-size:" + h1Size + ";font-weight:700;color:" + titleColor + ";margin-bottom:20px;line-height:1.4;}";
    }

    private String sanitizeHtmlContent(String content) {
        if (content == null) return "";
        // Remove style attributes
        content = content.replaceAll("\\s+style=\"[^\"]*\"", "");
        content = content.replaceAll("\\s+style='[^']*'", "");
        // Remove class attributes
        content = content.replaceAll("\\s+class=\"[^\"]*\"", "");
        content = content.replaceAll("\\s+class='[^']*'", "");
        return content;
    }

    private String wrapContentWithStyle(String title, String content, String styleCss) {
        String cleanContent = sanitizeHtmlContent(content);
        return "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>"
                + title + "</title>\n<style>"
                + styleCss
                + "</style>\n</head>\n<body>\n<div class=\"article-title\">"
                + title + "</div>\n<div class=\"article-content\">"
                + cleanContent
                + "</div>\n</body>\n</html>";
    }

    private String callClaudeForContent(String prompt) {
        return callClaudeForContent(prompt, new File(System.getProperty("user.home")));
    }

    private String callClaudeForContent(String prompt, File workingDir) {
        Process process = null;
        try {
            List<String> command = new ArrayList<>();
            command.add("claude");
            command.add("-p");
            command.add(prompt);
            command.add("--output-format=json");
            command.add("--no-session-persistence");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workingDir);
            pb.redirectErrorStream(true);
            pb.redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")));
            process = pb.start();

            final Process finalProcess = process;
            StringBuilder output = new StringBuilder();
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(finalProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                } catch (IOException ignored) {}
            });
            readerThread.start();

            boolean finished = process.waitFor(180, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                try {
                    process.descendants().forEach(ProcessHandle::destroyForcibly);
                } catch (Exception ignored) {}
                readerThread.interrupt();
                return null;
            }

            readerThread.join(5000);

            String rawOutput = output.toString().trim();
            if (rawOutput.isEmpty()) {
                return null;
            }

            // Try to parse JSON output; fall back to raw text if JSON parsing fails
            String content = extractContentFromClaudeOutput(rawOutput);
            if (content != null && !content.isEmpty()) {
                return content;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                try { process.descendants().forEach(ProcessHandle::destroyForcibly); } catch (Exception ignored) {}
            }
            return null;
        }
    }

    /**
     * Extract content from Claude output.
     * Handles both JSON-wrapped output (--output-format=json) and raw markdown/HTML output.
     */
    private String extractContentFromClaudeOutput(String rawOutput) {
        ObjectMapper mapper = new ObjectMapper();

        // Step 1: Try to parse outer JSON (from --output-format=json)
        String innerText = null;
        try {
            int jsonStart = rawOutput.indexOf('{');
            if (jsonStart >= 0) {
                String jsonPart = rawOutput.substring(jsonStart);
                JsonNode root = mapper.readTree(jsonPart);
                innerText = root.path("result").asText("");
            }
        } catch (Exception e) {
            // Not valid outer JSON, treat entire output as content
            innerText = rawOutput;
        }

        if (innerText == null || innerText.isEmpty()) {
            innerText = rawOutput;
        }

        // Step 2: Try to find {"content":"..."} inside the result
        try {
            int innerJsonStart = innerText.indexOf('{');
            if (innerJsonStart >= 0) {
                String jsonCandidate = innerText.substring(innerJsonStart);
                // Find the matching closing brace for the JSON object
                int braceCount = 0;
                int endPos = -1;
                for (int i = 0; i < jsonCandidate.length(); i++) {
                    char c = jsonCandidate.charAt(i);
                    if (c == '{') braceCount++;
                    else if (c == '}') {
                        braceCount--;
                        if (braceCount == 0) {
                            endPos = i + 1;
                            break;
                        }
                    }
                }
                if (endPos > 0) {
                    String jsonStr = jsonCandidate.substring(0, endPos);
                    JsonNode innerRoot = mapper.readTree(jsonStr);
                    String content = innerRoot.path("content").asText("");
                    if (!content.isEmpty()) {
                        return content;
                    }
                }
            }
        } catch (Exception e) {
            // innerText is not JSON, fall through to raw text extraction
        }

        // Step 3: Fall back - try to extract HTML content from raw text
        // Look for <div>...</div> or <h1>...</h1> or other HTML tags
        String text = innerText;

        // If the text starts with common prefixes, strip them
        String[] prefixesToStrip = {
            "我来为您根据这个标题生成一篇文章。",
            "我来为你生成一篇文章。",
            "好的，我来为您生成文章。",
            "以下是文章内容：",
            "文章如下：",
        };
        for (String prefix : prefixesToStrip) {
            if (text.startsWith(prefix)) {
                text = text.substring(prefix.length()).trim();
                break;
            }
        }

        // Strip markdown code block markers if present
        if (text.startsWith("```html")) {
            int start = text.indexOf("\n");
            int end = text.lastIndexOf("```");
            if (start > 0 && end > start) {
                text = text.substring(start + 1, end).trim();
            }
        } else if (text.startsWith("```")) {
            int start = text.indexOf("\n");
            int end = text.lastIndexOf("```");
            if (start > 0 && end > start) {
                text = text.substring(start + 1, end).trim();
            }
        }

        // If text contains HTML tags, wrap it in a div
        if (text.contains("<div") || text.contains("<h1") || text.contains("<h2") || text.contains("<p") || text.contains("<blockquote")) {
            // Already HTML-ish, return as-is (will be wrapped later)
            return text;
        }

        // If text is markdown, convert common markdown to HTML
        if (text.contains("# ") || text.contains("## ") || text.contains("> ")) {
            return markdownToHtml(text);
        }

        // Plain text - wrap in paragraph tags with line breaks
        String[] paragraphs = text.split("\n\n");
        StringBuilder html = new StringBuilder();
        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;
            if (para.startsWith("# ")) {
                html.append("<h1>").append(escapeHtml(para.substring(2).trim())).append("</h1>\n");
            } else if (para.startsWith("## ")) {
                html.append("<h2>").append(escapeHtml(para.substring(3).trim())).append("</h2>\n");
            } else {
                html.append("<p>").append(escapeHtml(para).replace("\n", "<br>")).append("</p>\n");
            }
        }
        return html.toString();
    }

    private String markdownToHtml(String markdown) {
        StringBuilder html = new StringBuilder();
        String[] lines = markdown.split("\n");
        boolean inBlockquote = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            if (trimmed.startsWith("# ")) {
                if (inBlockquote) { html.append("</blockquote>\n"); inBlockquote = false; }
                html.append("<h1>").append(escapeHtml(trimmed.substring(2))).append("</h1>\n");
            } else if (trimmed.startsWith("## ")) {
                if (inBlockquote) { html.append("</blockquote>\n"); inBlockquote = false; }
                html.append("<h2>").append(escapeHtml(trimmed.substring(3))).append("</h2>\n");
            } else if (trimmed.startsWith("### ")) {
                if (inBlockquote) { html.append("</blockquote>\n"); inBlockquote = false; }
                html.append("<h2>").append(escapeHtml(trimmed.substring(4))).append("</h2>\n");
            } else if (trimmed.startsWith("> ")) {
                if (!inBlockquote) { html.append("<blockquote>\n"); inBlockquote = true; }
                html.append("<p>").append(escapeHtml(trimmed.substring(2))).append("</p>\n");
            } else if (trimmed.isEmpty()) {
                if (inBlockquote) { html.append("</blockquote>\n"); inBlockquote = false; }
            } else {
                if (inBlockquote) { html.append("</blockquote>\n"); inBlockquote = false; }
                html.append("<p>").append(escapeHtml(trimmed)).append("</p>\n");
            }
        }
        if (inBlockquote) { html.append("</blockquote>\n"); }
        return html.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private void runGenerateTask(String taskId, int countPerCombo, String outputPath,
                                 List<String> selectedPlatforms, List<String> selectedTrackIds) {
        Map<String, Object> task = generateTasks.get(taskId);
        try {
            List<Track> allTracks = trackMapper.findAll();
            if (allTracks == null || allTracks.isEmpty()) {
                task.put("status", "failed");
                task.put("message", "系统中没有赛道数据");
                return;
            }

            List<Track> tracks;
            if (selectedTrackIds != null && !selectedTrackIds.isEmpty()) {
                tracks = allTracks.stream()
                        .filter(t -> selectedTrackIds.contains(t.getId()))
                        .collect(Collectors.toList());
            } else {
                tracks = allTracks;
            }

            if (tracks.isEmpty()) {
                task.put("status", "failed");
                task.put("message", "选择的赛道中没有可用数据");
                return;
            }

            List<String> platforms;
            if (selectedPlatforms != null && !selectedPlatforms.isEmpty()) {
                platforms = selectedPlatforms;
            } else {
                platforms = Arrays.asList("公众号", "今日头条", "百家号");
            }

            List<Map<String, String>> allRows = Collections.synchronizedList(new ArrayList<>());
            int totalBatches = 0;
            int completedBatches = 0;

            for (String platform : platforms) {
                List<Track> platformTracks = tracks.stream()
                        .filter(t -> t.getPlatforms() != null && t.getPlatforms().contains(platform))
                        .collect(Collectors.toList());
                if (!platformTracks.isEmpty()) {
                    totalBatches += (int) Math.ceil(platformTracks.size() / 5.0);
                }
            }

            final int finalTotalBatches = totalBatches;

            if (totalBatches == 0) {
                task.put("status", "failed");
                task.put("message", "所选平台和赛道没有可生成的组合，请检查平台与赛道的对应关系");
                return;
            }

            for (String platform : platforms) {
                List<Track> platformTracks = tracks.stream()
                        .filter(t -> t.getPlatforms() != null && t.getPlatforms().contains(platform))
                        .collect(Collectors.toList());

                if (platformTracks.isEmpty()) continue;

                int batchSize = 5;
                for (int batchStart = 0; batchStart < platformTracks.size(); batchStart += batchSize) {
                    Map<String, Object> currentTask = generateTasks.get(taskId);
                    if (currentTask != null && "cancelled".equals(currentTask.get("status"))) {
                        task.put("status", "cancelled");
                        task.put("message", "任务已取消");
                        return;
                    }

                    int batchEnd = Math.min(batchStart + batchSize, platformTracks.size());
                    List<Track> batchTracks = platformTracks.subList(batchStart, batchEnd);

                    task.put("message", "正在生成 " + platform + " 平台标题（批次 " + (batchStart / batchSize + 1) + "/" + (int) Math.ceil(platformTracks.size() / 5.0) + "）...");

                    StringBuilder prompt = new StringBuilder();
                    prompt.append("请为\"").append(platform).append("\"平台生成爆款标题。\n\n");
                    prompt.append("需要生成标题的赛道：\n");
                    for (int i = 0; i < batchTracks.size(); i++) {
                        Track t = batchTracks.get(i);
                        prompt.append(i + 1).append(". ").append(t.getName());
                        if (t.getIntro() != null && !t.getIntro().isEmpty()) {
                            prompt.append("：").append(t.getIntro());
                        }
                        prompt.append("\n");
                    }
                    prompt.append("\n每个赛道生成").append(countPerCombo).append("个标题。要求：\n");
                    prompt.append("1. 标题是爆款风格，吸引眼球，适合").append(platform).append("传播\n");
                    prompt.append("2. 每个标题必须配一段SEO描述（30-50字），要求：\n");
                    prompt.append("   - 包含赛道核心关键词，便于搜索引擎收录\n");
                    prompt.append("   - 突出文章价值点和读者收益\n");
                    prompt.append("   - 语言自然流畅，符合").append(platform).append("的搜索推荐算法偏好\n");
                    prompt.append("   - 适当使用数字、疑问、对比等提升点击率的手法\n");
                    prompt.append("3. 只输出纯JSON，不要markdown代码块，不要任何额外文字\n\n");
                    prompt.append("格式：{\"titles\":[{\"track\":\"赛道名称\",\"title\":\"标题文字\",\"description\":\"SEO描述\"},...]}");

                    JsonNode arr = callClaude(prompt.toString());
                    if (arr != null && arr.isArray()) {
                        for (JsonNode node : arr) {
                            Map<String, String> row = new HashMap<>();
                            row.put("title", node.path("title").asText(""));
                            row.put("platform", platform);
                            row.put("track", node.path("track").asText(""));
                            row.put("description", node.path("description").asText(""));
                            if (!row.get("title").isEmpty()) {
                                allRows.add(row);
                            }
                        }
                    }

                    completedBatches++;
                    int progress = finalTotalBatches > 0 ? (completedBatches * 100 / finalTotalBatches) : 0;
                    task.put("progress", progress);
                }
            }

            task.put("message", "正在写入 Excel 文件...");
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet("生成标题");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("标题");
            header.createCell(1).setCellValue("平台");
            header.createCell(2).setCellValue("赛道名称");
            header.createCell(3).setCellValue("描述");

            for (int i = 0; i < allRows.size(); i++) {
                Map<String, String> row = allRows.get(i);
                Row r = sheet.createRow(i + 1);
                r.createCell(0).setCellValue(row.get("title"));
                r.createCell(1).setCellValue(row.get("platform"));
                r.createCell(2).setCellValue(row.get("track"));
                r.createCell(3).setCellValue(row.get("description"));
            }

            for (int i = 0; i < 4; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }

            String finalPath = outputPath;
            File outFile = new File(outputPath);
            if (outFile.isDirectory() || !finalPath.toLowerCase().endsWith(".xlsx")) {
                if (outFile.isDirectory()) {
                    finalPath = outputPath + File.separator + "生成标题.xlsx";
                } else if (!finalPath.toLowerCase().endsWith(".xlsx")) {
                    finalPath = outputPath + ".xlsx";
                }
                outFile = new File(finalPath);
            }
            File parent = outFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                wb.write(fos);
            }
            wb.close();

            int savedCount = 0;
            for (Map<String, String> row : allRows) {
                String trackName = row.get("track");
                String trackId = null;
                if (trackName != null && !trackName.isEmpty()) {
                    Track t = trackMapper.findByName(trackName);
                    if (t != null) {
                        trackId = t.getId();
                    }
                }
                TitleLibrary tl = new TitleLibrary();
                tl.setTitle(row.get("title"));
                tl.setDescription(row.get("description"));
                tl.setPlatform(row.get("platform"));
                tl.setTrackId(trackId);
                tl.setUseCount(0);
                titleLibraryService.save(tl);
                savedCount++;
            }

            task.put("status", "completed");
            task.put("progress", 100);
            task.put("total", allRows.size());
            task.put("message", "生成完成，共 " + allRows.size() + " 条标题，已自动入库 " + savedCount + " 条");
            task.put("path", finalPath);

        } catch (Exception e) {
            e.printStackTrace();
            task.put("status", "failed");
            task.put("message", "生成失败：" + e.getMessage());
        }
    }

    private JsonNode callClaude(String prompt) {
        Process process = null;
        try {
            List<String> command = new ArrayList<>();
            command.add("claude");
            command.add("-p");
            command.add(prompt);
            command.add("--output-format=json");
            command.add("--no-session-persistence");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(System.getProperty("user.home")));
            pb.redirectErrorStream(true);
            pb.redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")));
            process = pb.start();

            final Process finalProcess = process;
            StringBuilder output = new StringBuilder();
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(finalProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                } catch (IOException ignored) {}
            });
            readerThread.start();

            boolean finished = process.waitFor(180, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                try {
                    process.descendants().forEach(ProcessHandle::destroyForcibly);
                } catch (Exception ignored) {}
                readerThread.interrupt();
                return null;
            }

            readerThread.join(5000);

            String rawOutput = output.toString().trim();
            if (rawOutput.isEmpty()) {
                return null;
            }

            String result = rawOutput;
            int jsonStart = rawOutput.indexOf('{');
            if (jsonStart > 0) {
                result = rawOutput.substring(jsonStart);
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(result);
            String innerJson = root.path("result").asText("");
            if (innerJson.isEmpty()) {
                return null;
            }
            // Some responses contain text before JSON; extract the JSON object
            int innerJsonStart = innerJson.indexOf('{');
            if (innerJsonStart >= 0) {
                innerJson = innerJson.substring(innerJsonStart);
            }
            // Find matching closing brace
            int braceCount = 0;
            int endPos = -1;
            for (int i = 0; i < innerJson.length(); i++) {
                char c = innerJson.charAt(i);
                if (c == '{') braceCount++;
                else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        endPos = i + 1;
                        break;
                    }
                }
            }
            if (endPos > 0) {
                innerJson = innerJson.substring(0, endPos);
            }
            JsonNode innerRoot = mapper.readTree(innerJson);
            return innerRoot.path("titles");
        } catch (Exception e) {
            e.printStackTrace();
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                try { process.descendants().forEach(ProcessHandle::destroyForcibly); } catch (Exception ignored) {}
            }
            return null;
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
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return null;
    }
}
