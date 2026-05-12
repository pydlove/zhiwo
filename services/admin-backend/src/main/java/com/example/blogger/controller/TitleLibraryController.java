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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.apache.poi.ss.usermodel.*;
import com.example.blogger.service.LLMService;
import com.example.blogger.util.DocxGenerator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/title-library")
@CrossOrigin(origins = "*")
public class TitleLibraryController {

    private static final Logger log = LoggerFactory.getLogger(TitleLibraryController.class);

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
    private final BannedWordMapper bannedWordMapper;
    private final com.example.blogger.service.TitleReviewService titleReviewService;
    private final ArticleFeedbackMapper articleFeedbackMapper;
    private final ServerConfigMapper serverConfigMapper;
    private final org.springframework.web.client.RestTemplate restTemplate;
    private final ConfigMapper configMapper;
    private final LLMService llmService;
    private final DocxGenerator docxGenerator;
    private final com.example.blogger.service.TitleGenerationTaskService titleGenerationTaskService;

    @Value("${app.script.replace-periods-path}")
    private String replacePeriodsScriptPath;

    @Value("${app.script.auto-insert-images-path:}")
    private String autoInsertImagesScriptPath;

    // Async generate task storage (for titles)
    private final ConcurrentHashMap<String, Map<String, Object>> generateTasks = new ConcurrentHashMap<>();
    // Async generate-post task storage (for subscription posts)
    private final ConcurrentHashMap<String, Map<String, Object>> generatePostTasks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    // 批量推送幂等性校验：记录正在处理中的请求指纹（key -> expireAtMs）
    private final ConcurrentHashMap<String, Long> pushInProgress = new ConcurrentHashMap<>();
    private static final long PUSH_IDEMPOTENCY_TTL_MS = 5 * 60 * 1000; // 5分钟

    private String buildPushFingerprint(String dateStr, List<String> userIds) {
        List<String> sorted = new ArrayList<>(userIds);
        Collections.sort(sorted);
        return dateStr + "|" + String.join(",", sorted);
    }

    private void cleanupExpiredPushFingerprints() {
        long now = System.currentTimeMillis();
        pushInProgress.entrySet().removeIf(e -> e.getValue() < now);
    }

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
                                  SubscriptionPostMapper subscriptionPostMapper,
                                  BannedWordMapper bannedWordMapper,
                                  com.example.blogger.service.TitleReviewService titleReviewService,
                                  ArticleFeedbackMapper articleFeedbackMapper,
                                  ServerConfigMapper serverConfigMapper,
                                  ConfigMapper configMapper,
                                  org.springframework.web.client.RestTemplate restTemplate,
                                  LLMService llmService,
                                  DocxGenerator docxGenerator,
                                  com.example.blogger.service.TitleGenerationTaskService titleGenerationTaskService) {
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
        this.bannedWordMapper = bannedWordMapper;
        this.titleReviewService = titleReviewService;
        this.articleFeedbackMapper = articleFeedbackMapper;
        this.serverConfigMapper = serverConfigMapper;
        this.configMapper = configMapper;
        this.restTemplate = restTemplate;
        this.llmService = llmService;
        this.docxGenerator = docxGenerator;
        this.titleGenerationTaskService = titleGenerationTaskService;
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

    @PostConstruct
    public void migrateIsUsedColumn() {
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, null, "tu_title_library", "is_used")) {
            if (!rs.next()) {
                conn.createStatement().execute("ALTER TABLE tu_title_library ADD COLUMN is_used TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已使用' AFTER use_count");
                System.out.println("Migration applied: added is_used column to tu_title_library");
            }
        } catch (SQLException e) {
            System.err.println("Migration check failed for is_used: " + e.getMessage());
        }
    }

    @PostConstruct
    public void migrateEmailPushLogTable() {
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, "tu_email_push_log", null)) {
            if (!rs.next()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS tu_email_push_log (" +
                    "  id VARCHAR(32) PRIMARY KEY," +
                    "  user_id VARCHAR(32) NOT NULL COMMENT '用户ID'," +
                    "  push_date DATE NOT NULL COMMENT '推送日期'," +
                    "  type VARCHAR(32) NOT NULL DEFAULT 'daily_recommend' COMMENT '推送类型'," +
                    "  title_library_id VARCHAR(32) COMMENT '关联标题库ID'," +
                    "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "  INDEX idx_user_date (user_id, push_date)," +
                    "  INDEX idx_push_date (push_date)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件推送日志表'"
                );
                System.out.println("Migration applied: created tu_email_push_log table");
            }
        } catch (SQLException e) {
            System.err.println("Migration check failed for tu_email_push_log: " + e.getMessage());
        }
    }

    @PostConstruct
    public void migrateArticleFeedbackTable() {
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, "tu_article_feedback", null)) {
            if (!rs.next()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS tu_article_feedback (" +
                    "  id VARCHAR(64) PRIMARY KEY," +
                    "  track_id VARCHAR(64) COMMENT '赛道ID'," +
                    "  platform VARCHAR(50) COMMENT '平台'," +
                    "  content TEXT NOT NULL COMMENT '反馈内容'," +
                    "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "  INDEX idx_track_platform (track_id, platform)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章反馈记忆表'"
                );
                System.out.println("Migration applied: created tu_article_feedback table");
            }
        } catch (SQLException e) {
            System.err.println("Migration check failed for tu_article_feedback: " + e.getMessage());
        }
    }

    /**
     * 多运营者体系：添加必要的字段
     */
    @PostConstruct
    public void migrateOperatorColumns() {
        try (Connection conn = dataSource.getConnection()) {
            // 1. ta_user 增加 qr_code_url
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "ta_user", "qr_code_url")) {
                if (!rs.next()) {
                    conn.createStatement().execute("ALTER TABLE ta_user ADD COLUMN qr_code_url VARCHAR(500) COMMENT '客服二维码图片URL'");
                    System.out.println("Migration applied: added qr_code_url to ta_user");
                }
            }
            // 2. tu_user 增加 admin_id
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "tu_user", "admin_id")) {
                if (!rs.next()) {
                    conn.createStatement().execute("ALTER TABLE tu_user ADD COLUMN admin_id VARCHAR(64) COMMENT '归属运营者(admin_id)'");
                    System.out.println("Migration applied: added admin_id to tu_user");
                }
            }
            // 3. tu_customer_dialogue 增加 admin_id
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "tu_customer_dialogue", "admin_id")) {
                if (!rs.next()) {
                    conn.createStatement().execute("ALTER TABLE tu_customer_dialogue ADD COLUMN admin_id VARCHAR(64) COMMENT '归属运营者ID, null=系统默认'");
                    System.out.println("Migration applied: added admin_id to tu_customer_dialogue");
                }
            }
        } catch (SQLException e) {
            System.err.println("Migration check failed for operator columns: " + e.getMessage());
        }
    }

    @PostConstruct
    public void migrateTitleLibraryGeneratedFields() {
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, null, "tu_title_library", "generated_file_url")) {
            if (!rs.next()) {
                conn.createStatement().execute("ALTER TABLE tu_title_library ADD COLUMN generated_file_url VARCHAR(255) DEFAULT NULL");
                System.out.println("Migration applied: added generated_file_url column to tu_title_library");
            }
        } catch (SQLException e) {
            System.err.println("Migration check failed for generated_file_url: " + e.getMessage());
        }
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, null, "tu_title_library", "generated_file_name")) {
            if (!rs.next()) {
                conn.createStatement().execute("ALTER TABLE tu_title_library ADD COLUMN generated_file_name VARCHAR(255) DEFAULT NULL");
                System.out.println("Migration applied: added generated_file_name column to tu_title_library");
            }
        } catch (SQLException e) {
            System.err.println("Migration check failed for generated_file_name: " + e.getMessage());
        }
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, null, "tu_title_library", "generated_at")) {
            if (!rs.next()) {
                conn.createStatement().execute("ALTER TABLE tu_title_library ADD COLUMN generated_at DATETIME DEFAULT NULL");
                System.out.println("Migration applied: added generated_at column to tu_title_library");
            }
        } catch (SQLException e) {
            System.err.println("Migration check failed for generated_at: " + e.getMessage());
        }
    }

    @PostConstruct
    public void migrateTitleGenerationTaskTable() {
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, "tu_title_generation_task", null)) {
            if (!rs.next()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS tu_title_generation_task (" +
                    "  id VARCHAR(64) PRIMARY KEY," +
                    "  title_library_id VARCHAR(64) NOT NULL COMMENT '关联的标题库ID'," +
                    "  title VARCHAR(500) COMMENT '标题内容'," +
                    "  prompt TEXT COMMENT '使用的提示词'," +
                    "  status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending/processing/completed/failed'," +
                    "  result_file_url VARCHAR(500) COMMENT '生成的文件URL'," +
                    "  result_file_name VARCHAR(255) COMMENT '生成的文件名'," +
                    "  error_message TEXT COMMENT '错误信息'," +
                    "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "  processed_at DATETIME," +
                    "  INDEX idx_title_library_id (title_library_id)," +
                    "  INDEX idx_status (status)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标题生成任务表'"
                );
                System.out.println("Migration applied: created tu_title_generation_task table");
            }
        } catch (SQLException e) {
            System.err.println("Migration check failed for tu_title_generation_task: " + e.getMessage());
        }
    }

    /**
     * 获取用户当前激活的赛道ID集合（按订阅时间先后，只保留前 trackLimit 个）
     * trackLimit <= 0 视为无限制，使用所有订阅赛道
     */
    private Set<String> getActiveTrackIdsForUser(User user) {
        List<UserTrack> uts = userTrackMapper.findByUserId(user.getId());
        if (uts == null || uts.isEmpty()) {
            return Collections.emptySet();
        }
        int limit = user.getTrackLimit() != null ? user.getTrackLimit() : uts.size();
        // trackLimit <= 0 视为无限制，使用所有订阅赛道
        if (limit <= 0) {
            limit = uts.size();
        }
        // findByUserId 已按 created_at ASC 排序，最早订阅的优先保留
        Set<String> active = new LinkedHashSet<>();
        for (int i = 0; i < Math.min(limit, uts.size()); i++) {
            active.add(uts.get(i).getTrackId());
        }
        return active;
    }

    /**
     * 诊断接口：查询指定用户为何匹配不上指定标题
     */
    @GetMapping("/debug-match")
    public Result<Map<String, Object>> debugMatch(
            @RequestParam("userId") String userId,
            @RequestParam(value = "titleId", required = false) String titleId) {
        Map<String, Object> result = new HashMap<>();
        User user = userMapper.findById(userId);
        if (user == null) {
            return Result.error("用户不存在: " + userId);
        }

        result.put("userId", userId);
        result.put("username", user.getUsername());
        result.put("platformLimit", user.getPlatformLimit());
        result.put("trackLimit", user.getTrackLimit());

        List<UserTrack> uts = userTrackMapper.findByUserId(userId);
        List<Map<String, Object>> tracks = new ArrayList<>();
        if (uts != null) {
            for (UserTrack ut : uts) {
                Track track = trackMapper.findById(ut.getTrackId());
                Map<String, Object> t = new HashMap<>();
                t.put("trackId", ut.getTrackId());
                t.put("trackName", track != null ? track.getName() : "未知");
                t.put("platforms", track != null ? track.getPlatforms() : "未知");
                tracks.add(t);
            }
        }
        result.put("subscribedTracks", tracks);

        Set<String> activeTrackIds = getActiveTrackIdsForUser(user);
        result.put("activeTrackIds", activeTrackIds);

        if (titleId != null && !titleId.isEmpty()) {
            TitleLibrary title = titleLibraryService.getById(titleId);
            if (title != null) {
                Map<String, Object> titleInfo = new HashMap<>();
                titleInfo.put("titleId", titleId);
                titleInfo.put("title", title.getTitle());
                titleInfo.put("platform", title.getPlatform());
                titleInfo.put("trackId", title.getTrackId());
                result.put("title", titleInfo);

                List<String> reasons = new ArrayList<>();
                boolean trackIdMatched = false;
                boolean titleNameMatched = false;
                if (title.getTrackId() != null && !title.getTrackId().isEmpty()) {
                    if (activeTrackIds.contains(title.getTrackId())) {
                        trackIdMatched = true;
                    }
                }
                // Fallback: check if title name contains any active track name
                if (!trackIdMatched && title.getTitle() != null && !title.getTitle().isEmpty()) {
                    for (String atid : activeTrackIds) {
                        Track at = trackMapper.findById(atid);
                        if (at != null && at.getName() != null && !at.getName().isEmpty()
                                && title.getTitle().contains(at.getName())) {
                            titleNameMatched = true;
                            break;
                        }
                    }
                }
                if (!trackIdMatched && !titleNameMatched) {
                    reasons.add("赛道不匹配: trackId=" + title.getTrackId() + " 不在用户活跃赛道 " + activeTrackIds + " 中，且标题名称也不包含任一订阅赛道名称");
                }
                result.put("matchBlockReasons", reasons);
                result.put("canMatch", reasons.isEmpty());
                result.put("trackIdMatched", trackIdMatched);
                result.put("titleNameMatched", titleNameMatched);
            } else {
                result.put("title", "标题不存在");
            }
        }

        return Result.ok(result);
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
            @RequestParam(value = "isUsed", required = false) String isUsed,
            @RequestParam(value = "userType", required = false) String userType,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortOrder", required = false) String sortOrder) {
        boolean hasFilter = (platform != null && !platform.isEmpty())
                || (trackId != null && !trackId.isEmpty())
                || (keyword != null && !keyword.isEmpty())
                || (recommendUserName != null && !recommendUserName.isEmpty())
                || (matched != null && !matched.isEmpty())
                || (pushDate != null && !pushDate.isEmpty())
                || (isUsed != null && !isUsed.isEmpty())
                || (userType != null && !userType.isEmpty());
        if (page != null && pageSize != null && page > 0 && pageSize > 0) {
            if (hasFilter) {
                return Result.ok(titleLibraryService.searchPage(platform, trackId, keyword, recommendUserName, matched, pushDate, isUsed, userType, page, pageSize, sortField, sortOrder));
            }
            return Result.ok(titleLibraryService.listPage(page, pageSize));
        }
        if (hasFilter) {
            return Result.ok(titleLibraryService.search(platform, trackId, keyword, recommendUserName, matched, pushDate, isUsed, userType, sortField, sortOrder));
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

    @PostMapping("/batch-change-track")
    public Result<Map<String, Object>> batchChangeTrack(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> titleIds = (List<String>) body.get("titleIds");
        String trackId = (String) body.get("trackId");
        if (titleIds == null || titleIds.isEmpty()) {
            return Result.error("请选择要修改的标题");
        }
        if (trackId == null || trackId.isEmpty()) {
            return Result.error("请选择赛道");
        }
        int success = 0;
        int failed = 0;
        for (String id : titleIds) {
            try {
                TitleLibrary tl = titleLibraryService.getById(id);
                if (tl == null) {
                    failed++;
                    continue;
                }
                tl.setTrackId(trackId);
                titleLibraryService.save(tl);
                success++;
            } catch (Exception e) {
                failed++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        return Result.ok(result);
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
            @RequestParam(value = "isUsed", required = false) String isUsed,
            @RequestParam(value = "userType", required = false) String userType,
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
                        || (pushDate != null && !pushDate.isEmpty())
                        || (isUsed != null && !isUsed.isEmpty());
                titles = hasFilter
                        ? titleLibraryService.search(platform, trackId, keyword, recommendUserName, matched, pushDate, isUsed, userType, null, null)
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
            @RequestParam(value = "isUsed", required = false) String isUsed,
            @RequestParam(value = "userType", required = false) String userType,
            @RequestParam(value = "titleIds", required = false) List<String> titleIds,
            @RequestParam(value = "baseName", required = false) String baseName) {
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
                        || (pushDate != null && !pushDate.isEmpty())
                        || (isUsed != null && !isUsed.isEmpty());
                titles = hasFilter
                        ? titleLibraryService.search(platform, trackId, keyword, recommendUserName, matched, pushDate, isUsed, userType, null, null)
                        : titleLibraryService.list();
            }

            if (titles == null || titles.isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "没有可导出的数据");
                return;
            }

            // Build rule content once (shared across all files)
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

            List<com.example.blogger.entity.BannedWord> bannedWords = bannedWordMapper.findAll();
            if (bannedWords != null && !bannedWords.isEmpty()) {
                StringBuilder sb = new StringBuilder("\n\n【违禁词约束】\n");
                List<String> blockWords = new ArrayList<>();
                List<String> cautionWords = new ArrayList<>();
                List<String> replacements = new ArrayList<>();
                for (com.example.blogger.entity.BannedWord bw : bannedWords) {
                    if ("block".equals(bw.getSeverity())) {
                        blockWords.add(bw.getWord());
                    } else {
                        cautionWords.add(bw.getWord());
                    }
                    if (bw.getReplacement() != null && !bw.getReplacement().isEmpty()) {
                        replacements.add("\"" + bw.getWord() + "\" → \"" + bw.getReplacement() + "\"");
                    }
                }
                if (!blockWords.isEmpty()) {
                    sb.append("以下内容严禁出现：").append(String.join("、", blockWords)).append("\n");
                }
                if (!cautionWords.isEmpty()) {
                    sb.append("以下内容慎用：").append(String.join("、", cautionWords)).append("\n");
                }
                if (!replacements.isEmpty()) {
                    sb.append("如需表达类似含义，请使用替换词：\n");
                    for (String r : replacements) {
                        sb.append("  - ").append(r).append("\n");
                    }
                }
                ruleContent = ruleContent + sb.toString();
            }

            String effectiveBaseName = (baseName != null && !baseName.isEmpty()) ? baseName : "标题库导出";

            response.setContentType("application/zip");
            String zipFileName = effectiveBaseName + ".zip";
            response.setHeader("Content-Disposition", "attachment; filename=" + new String(zipFileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));

            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {
                String[] headers = { "ID", "标题名称", "用户名", "样式风格", "描述", "推荐日期", "是否创作完成" };

                List<com.example.blogger.entity.Style> allStyles = styleMapper.findAll();
                Random styleRandom = new Random();

                for (int idx = 0; idx < titles.size(); idx++) {
                    TitleLibrary tl = titles.get(idx);

                    XSSFWorkbook wb = new XSSFWorkbook();

                    // Title sheet (single row)
                    String sheetName = tl.getPlatform() != null && !tl.getPlatform().isEmpty() ? tl.getPlatform() : "未分类";
                    if (tl.getTrackName() != null && !tl.getTrackName().isEmpty()) {
                        sheetName = sheetName + "-" + tl.getTrackName();
                    }
                    if (sheetName.length() > 31) {
                        sheetName = sheetName.substring(0, 31);
                    }
                    sheetName = sheetName.replace(":", "-").replace("\\", "-").replace("/", "-").replace("?", "-").replace("*", "-").replace("[", "(").replace("]", ")");

                    Sheet sheet = wb.createSheet(sheetName);
                    Row headerRow = sheet.createRow(0);
                    for (int i = 0; i < headers.length; i++) {
                        headerRow.createCell(i).setCellValue(headers[i]);
                    }
                    Row row = sheet.createRow(1);
                    row.createCell(0).setCellValue(tl.getId() != null ? tl.getId() : "");
                    row.createCell(1).setCellValue(tl.getTitle() != null ? tl.getTitle() : "");
                    row.createCell(2).setCellValue(tl.getRecommendUserName() != null ? tl.getRecommendUserName() : "");
                    String styleValue = tl.getRecommendUserTemplate();
                    if ((styleValue == null || styleValue.isEmpty()) && !allStyles.isEmpty()) {
                        styleValue = allStyles.get(styleRandom.nextInt(allStyles.size())).getName();
                    }
                    row.createCell(3).setCellValue(styleValue != null ? styleValue : "");
                    row.createCell(4).setCellValue(tl.getDescription() != null ? tl.getDescription() : "");
                    row.createCell(5).setCellValue(tl.getRecommendDate() != null ? tl.getRecommendDate().toString() : "");
                    boolean completed = tl.getSubscriptionPostTitle() != null && !tl.getSubscriptionPostTitle().isEmpty()
                            || tl.getSubscriptionPostFileUrl() != null && !tl.getSubscriptionPostFileUrl().isEmpty();
                    row.createCell(6).setCellValue(completed ? "是" : "");
                    for (int i = 0; i < headers.length; i++) {
                        sheet.setColumnWidth(i, 20 * 256);
                    }

                    // Rule sheet
                    Sheet ruleSheet = wb.createSheet("生成规则");
                    Row ruleRow = ruleSheet.createRow(0);
                    Cell ruleCell = ruleRow.createCell(0);
                    ruleCell.setCellValue(ruleContent);
                    ruleSheet.setColumnWidth(0, 60 * 256);

                    // Banned words sheet
                    if (bannedWords != null && !bannedWords.isEmpty()) {
                        Sheet bannedSheet = wb.createSheet("违禁词映射");
                        String[] bannedHeaders = { "违禁词", "替换词", "分类", "等级" };
                        Row bannedHeaderRow = bannedSheet.createRow(0);
                        for (int i = 0; i < bannedHeaders.length; i++) {
                            bannedHeaderRow.createCell(i).setCellValue(bannedHeaders[i]);
                        }
                        for (int i = 0; i < bannedWords.size(); i++) {
                            com.example.blogger.entity.BannedWord bw = bannedWords.get(i);
                            Row br = bannedSheet.createRow(i + 1);
                            br.createCell(0).setCellValue(bw.getWord() != null ? bw.getWord() : "");
                            br.createCell(1).setCellValue(bw.getReplacement() != null ? bw.getReplacement() : "");
                            br.createCell(2).setCellValue(bw.getCategory() != null ? bw.getCategory() : "");
                            br.createCell(3).setCellValue("block".equals(bw.getSeverity()) ? "严禁" : "慎用");
                        }
                        for (int i = 0; i < bannedHeaders.length; i++) {
                            bannedSheet.setColumnWidth(i, 20 * 256);
                        }
                    }

                    // Write workbook to zip entry
                    String seq = String.format("%02d", idx + 1);
                    String entryName = effectiveBaseName + "-" + seq + ".xlsx";
                    java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(entryName);
                    zos.putNextEntry(zipEntry);
                    try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
                        wb.write(bos);
                        zos.write(bos.toByteArray());
                    }
                    zos.closeEntry();
                    wb.close();
                }
                zos.finish();
            }

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
                String safeBase = baseName.replaceAll("[\\\\/:*?\"<>|\u201C\u201D]", "_").trim();
                if (safeBase.isEmpty()) {
                    safeBase = matchedTitle.getTitle() != null ? matchedTitle.getTitle().replaceAll("[\\\\/:*?\"<>|\u201C\u201D]", "_").trim() : "article";
                }
                String fileName = safeBase + "." + ext;
                System.out.println("[ImportArticle] originalName=" + originalName + ", safeBase=" + safeBase + ", fileName=" + fileName);
                String filePath = articlesDir + File.separator + fileName;

                // 1. Save file first to avoid stream conflicts
                file.transferTo(new File(filePath));

                // 2. Extract text from the saved file
                String extractedText = "";
                try (InputStream is = new FileInputStream(filePath)) {
                    if ("docx".equals(ext)) {
                        XWPFDocument document = new XWPFDocument(is);
                        StringBuilder sb = new StringBuilder();
                        for (XWPFParagraph para : document.getParagraphs()) {
                            sb.append(para.getText()).append("\n");
                        }
                        extractedText = sb.toString();
                        document.close();
                    } else if ("doc".equals(ext)) {
                        HWPFDocument document = new HWPFDocument(is);
                        WordExtractor extractor = new WordExtractor(document);
                        extractedText = extractor.getText();
                        extractor.close();
                    }
                    System.out.println("[ImportArticle] 文本提取成功, 长度=" + extractedText.length() + ", file=" + originalName);
                } catch (Exception ex) {
                    System.err.println("[ImportArticle] 文本提取失败: " + originalName);
                    ex.printStackTrace();
                }

                // 3. Replace Chinese periods with commas, except when followed by newline
                String processedText = extractedText;
                if (!extractedText.isEmpty()) {
                    processedText = extractedText.replaceAll("。(?![\\r\\n])", "，");
                    int changed = 0;
                    for (int i = 0; i < extractedText.length() && i < processedText.length(); i++) {
                        if (extractedText.charAt(i) != processedText.charAt(i)) changed++;
                    }
                    System.out.println("[ImportArticle] 句号替换完成, 替换数量=" + changed + ", file=" + originalName);
                }

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
                post.setDescription(processedText);
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

    @PostMapping("/import-articles-from-dir")
    public Result<Map<String, Object>> importArticlesFromDir(@RequestBody Map<String, String> req) {
        String dirPath = req.get("dirPath");
        if (dirPath == null || dirPath.isEmpty()) {
            return Result.error("请填写目录路径");
        }
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return Result.error("目录不存在或不是有效目录: " + dirPath);
        }

        // 递归收集所有 .doc/.docx 文件
        List<File> allFiles = new ArrayList<>();
        collectFiles(dir, allFiles);
        File[] files = allFiles.toArray(new File[0]);
        if (files == null || files.length == 0) {
            return Result.error("该目录下未找到 .doc/.docx 文件");
        }

        List<TitleLibrary> allTitles = titleLibraryService.list();
        String articlesDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "articles";
        File articlesDirFile = new File(articlesDir);
        if (!articlesDirFile.exists()) {
            articlesDirFile.mkdirs();
        }

        int success = 0;
        int skip = 0;
        List<Map<String, String>> errors = new ArrayList<>();
        List<Map<String, String>> details = new ArrayList<>();

        for (File file : files) {
            Map<String, Object> singleResult = processSingleArticleFile(file, allTitles, articlesDir);
            String status = (String) singleResult.get("status");
            if ("success".equals(status)) {
                success++;
                details.add(Map.of(
                    "file", (String) singleResult.get("file"),
                    "title", (String) singleResult.get("title"),
                    "user", (String) singleResult.getOrDefault("user", "")
                ));
            } else {
                skip++;
                errors.add(Map.of(
                    "file", (String) singleResult.get("file"),
                    "reason", (String) singleResult.get("reason")
                ));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("skip", skip);
        result.put("errors", errors);
        result.put("details", details);
        return Result.ok(result);
    }

    private void collectFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collectFiles(f, result);
            } else {
                String lower = f.getName().toLowerCase();
                if (lower.endsWith(".doc") || lower.endsWith(".docx")) {
                    result.add(f);
                }
            }
        }
    }

    /**
     * 解析服务器配置，优先使用默认配置
     */
    private ServerConfig resolveServerConfig(String serverConfigId) {
        // 1. 如果传了具体配置ID，直接使用
        if (serverConfigId != null && !serverConfigId.isEmpty()) {
            ServerConfig config = serverConfigMapper.findById(serverConfigId);
            if (config != null && (config.getIsActive() == null || Integer.valueOf(1).equals(config.getIsActive()))) {
                log.info("[resolveServerConfig] 使用指定配置: id={}, name={}", config.getId(), config.getName());
                return config;
            }
            log.warn("[resolveServerConfig] 指定配置不存在或未启用: id={}", serverConfigId);
        }
        // 2. 查找默认配置
        ServerConfig defaultConfig = serverConfigMapper.findDefault();
        if (defaultConfig != null) {
            log.info("[resolveServerConfig] 使用默认配置: id={}, name={}", defaultConfig.getId(), defaultConfig.getName());
            return defaultConfig;
        }
        // 3. 返回第一个启用配置
        List<ServerConfig> activeConfigs = serverConfigMapper.findAllActive();
        if (activeConfigs != null && !activeConfigs.isEmpty()) {
            log.info("[resolveServerConfig] 使用第一个启用配置: id={}, name={}", activeConfigs.get(0).getId(), activeConfigs.get(0).getName());
            return activeConfigs.get(0);
        }
        log.info("[resolveServerConfig] 没有可用的服务器配置，将本地处理");
        return null;
    }

    /**
     * 获取目标服务的基础地址
     */
    private String getBaseUrl(ServerConfig config) {
        String host = config.getHost();
        Integer port = config.getPort();
        String scheme = (port == 443 || port == 8443) ? "https" : "http";
        if (host.startsWith("http://") || host.startsWith("https://")) {
            scheme = host.startsWith("https") ? "https" : "http";
            host = host.replaceFirst("^https?://", "");
        }
        int p = (port == null || port == 80 || port == 443) ? (port == 443 || port == 8443 ? 8443 : 8080) : port;
        return scheme + "://" + host + ":" + p;
    }

    private Map<String, Object> processSingleArticleFile(File file, List<TitleLibrary> allTitles, String articlesDir) {
        String originalName = file.getName();
        String baseName = originalName;
        int lastSlash = Math.max(baseName.lastIndexOf('/'), baseName.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            baseName = baseName.substring(lastSlash + 1);
        }

        String ext = "";
        int lastDot = baseName.lastIndexOf('.');
        if (lastDot > 0) {
            ext = baseName.substring(lastDot + 1).toLowerCase();
            baseName = baseName.substring(0, lastDot);
        }
        if (!"doc".equals(ext) && !"docx".equals(ext)) {
            return Map.of("status", "skip", "file", originalName, "reason", "不是 .doc/.docx 文件");
        }

        String cleanName = baseName.replaceAll("^\\d+[-_.\\s]*", "").trim().toLowerCase();
        if (cleanName.isEmpty()) {
            cleanName = baseName.trim().toLowerCase();
        }

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
            return Map.of("status", "skip", "file", originalName, "reason", "未找到匹配的标题记录（最佳相似度：" + String.format("%.2f", bestScore) + "）");
        }

        TitleRecommendation rec = titleRecommendationMapper.findLatestByTitleId(matchedTitle.getId());
        if (rec == null) {
            return Map.of("status", "skip", "file", originalName, "reason", "标题「" + matchedTitle.getTitle() + "」没有关联的推荐记录");
        }

        String safeBase = baseName.replaceAll("[\\\\/:*?\"<>|\"\"]", "_").trim();
        if (safeBase.isEmpty()) {
            safeBase = matchedTitle.getTitle() != null ? matchedTitle.getTitle().replaceAll("[\\\\/:*?\"<>|\"\"]", "_").trim() : "article";
        }
        String fileName = safeBase + "." + ext;
        String filePath = articlesDir + File.separator + fileName;

        // Copy file to articles dir
        try {
            java.nio.file.Files.copy(file.toPath(), new File(filePath).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            return Map.of("status", "skip", "file", originalName, "reason", "文件复制失败：" + e.getMessage());
        }

        // Extract text
        String extractedText = "";
        try (InputStream is = new FileInputStream(filePath)) {
            if ("docx".equals(ext)) {
                XWPFDocument document = new XWPFDocument(is);
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph para : document.getParagraphs()) {
                    sb.append(para.getText()).append("\n");
                }
                extractedText = sb.toString();
                document.close();
            } else if ("doc".equals(ext)) {
                HWPFDocument document = new HWPFDocument(is);
                WordExtractor extractor = new WordExtractor(document);
                extractedText = extractor.getText();
                extractor.close();
            }
        } catch (Exception ex) {
            System.err.println("[ImportArticleFromDir] 文本提取失败: " + originalName);
            ex.printStackTrace();
        }

        // Replace periods
        String processedText = extractedText;
        if (!extractedText.isEmpty()) {
            processedText = extractedText.replaceAll("。(?![\\r\\n])", "，");
        }

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
        post.setDescription(processedText);
        post.setFileUrl(fileUrl);
        post.setFileName(fileName);
        post.setStatus("已上架");
        post.setUsed(0);
        subscriptionPostService.save(post);

        titleRecommendationMapper.updateSubscriptionPostId(rec.getId(), post.getId());

        return Map.of(
            "status", "success",
            "file", originalName,
            "title", matchedTitle.getTitle(),
            "user", rec.getUserName() != null ? rec.getUserName() : ""
        );
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

    /**
     * 匹配前预检测：计算供需缺口
     */
    @GetMapping("/match-check")
    public Result<Map<String, Object>> matchCheck(@RequestParam(value = "date", required = false) String dateStr) {
        try {
            LocalDate targetDate = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();

            // ===== 1. 计算标题供给（按 trackId 分组） =====
            List<TitleLibrary> allTitles = titleLibraryService.list();
            Set<String> allMatchedTitleIds = new HashSet<>(titleRecommendationMapper.findAllMatchedTitleIds());
            Map<String, Long> supplyMap = allTitles.stream()
                    .filter(t -> !allMatchedTitleIds.contains(t.getId())) // 过滤掉已绑定用户的标题
                    .filter(t -> t.getPushDate() == null || !t.getPushDate().isAfter(targetDate))
                    .filter(t -> t.getTrackId() != null && !t.getTrackId().isEmpty())
                    .collect(Collectors.groupingBy(TitleLibrary::getTrackId, Collectors.counting()));

            // ===== 2. 计算需求（每个用户的活跃赛道） =====
            List<User> users = userMapper.findAll().stream()
                    .filter(u -> u.getStatus() != null && u.getStatus() == 1)
                    .filter(u -> u.getIsDeleted() == null || u.getIsDeleted() != 1)
                    .filter(u -> u.getUserType() != null && u.getUserType() >= 1 && u.getUserType() <= 3)
                    .collect(Collectors.toList());

            // 排除当天已有推荐组合的用户-赛道
            Set<String> existingCombos = new HashSet<>();
            for (Map<String, Object> row : titleRecommendationMapper.findUserTrackCombosByDate(targetDate)) {
                String uid = (String) row.get("user_id");
                String tid = (String) row.get("track_id");
                if (uid != null && tid != null) existingCombos.add(uid + ":" + tid);
            }

            Map<String, Integer> demandMap = new HashMap<>();
            int totalCombos = 0;
            int userWithNoTracks = 0;

            for (User user : users) {
                Set<String> activeTracks = getActiveTrackIdsForUser(user);
                if (activeTracks.isEmpty()) {
                    userWithNoTracks++;
                    continue;
                }
                for (String trackId : activeTracks) {
                    String comboKey = user.getId() + ":" + trackId;
                    if (existingCombos.contains(comboKey)) continue; // 当天已有推荐，不计入需求
                    demandMap.put(trackId, demandMap.getOrDefault(trackId, 0) + 1);
                    totalCombos++;
                }
            }

            // ===== 3. 构建 trackId -> trackName 映射 =====
            List<Track> allTracks = trackMapper.findAll();
            Map<String, String> trackNameMap = new HashMap<>();
            for (Track t : allTracks) {
                if (t.getName() != null) trackNameMap.put(t.getId(), t.getName());
            }

            // ===== 3.5 加载每个用户的历史绑定标题ID，计算历史绑定影响 =====
            Map<String, Set<String>> userHistoryMap = new HashMap<>();
            int historyBoundTitleCount = 0;
            int historyBoundUserCount = 0;
            for (User user : users) {
                List<String> historyIds = titleRecommendationMapper.findHistoricallyMatchedTitleIdsByUserId(user.getId());
                Set<String> historySet = new HashSet<>(historyIds);
                userHistoryMap.put(user.getId(), historySet);
                // 统计该用户历史上绑定过多少当前可用的标题
                int bound = 0;
                for (TitleLibrary t : allTitles) {
                    if (historySet.contains(t.getId())) bound++;
                }
                if (bound > 0) {
                    historyBoundTitleCount += bound;
                    historyBoundUserCount++;
                }
            }

            // ===== 4. 计算缺口 =====
            List<Map<String, Object>> gaps = new ArrayList<>();
            List<Map<String, Object>> comboStats = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : demandMap.entrySet()) {
                String trackId = entry.getKey();
                int demand = entry.getValue();
                long supply = supplyMap.getOrDefault(trackId, 0L);
                Map<String, Object> stat = new HashMap<>();
                stat.put("trackId", trackId);
                stat.put("trackName", trackNameMap.getOrDefault(trackId, "未知赛道"));
                stat.put("demand", demand);
                stat.put("supply", (int) supply);
                stat.put("gap", Math.max(0, demand - supply));
                stat.put("sufficient", supply >= demand);
                comboStats.add(stat);
                if (supply < demand) {
                    Map<String, Object> gap = new HashMap<>();
                    gap.put("trackId", trackId);
                    gap.put("trackName", trackNameMap.getOrDefault(trackId, "未知赛道"));
                    gap.put("demand", demand);
                    gap.put("supply", (int) supply);
                    gap.put("need", demand - supply);
                    gaps.add(gap);
                }
            }

            // 按缺口从大到小排序
            gaps.sort((a, b) -> (Integer.valueOf(((Number) b.get("need")).intValue())).compareTo(((Number) a.get("need")).intValue()));
            comboStats.sort((a, b) -> (Integer.valueOf(((Number) b.get("gap")).intValue())).compareTo(((Number) a.get("gap")).intValue()));

            Map<String, Object> result = new HashMap<>();
            result.put("targetDate", targetDate.toString());
            result.put("totalUsers", users.size());
            result.put("totalCombos", totalCombos);
            result.put("userWithNoTracks", userWithNoTracks);
            result.put("comboStats", comboStats);
            result.put("gaps", gaps);
            result.put("gapCount", gaps.size());
            result.put("canMatch", gaps.isEmpty());
            result.put("existingCombosCount", existingCombos.size());
            result.put("historyBoundUserCount", historyBoundUserCount);
            result.put("historyBoundTitleCount", historyBoundTitleCount);
            return Result.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("检测失败：" + e.getMessage());
        }
    }

    @PostMapping("/match-today")
    public Result<Map<String, Object>> matchToday(@RequestParam(value = "date", required = false) String dateStr) {
        try {
            LocalDate targetDate;
            if (dateStr != null && !dateStr.isEmpty()) {
                targetDate = LocalDate.parse(dateStr);
            } else {
                targetDate = LocalDate.now();
            }
            List<TitleLibrary> allTitles = titleLibraryService.list();

            // ===== 过滤1：已绑定用户的标题，视为已使用，不再参与匹配 =====
            Set<String> allMatchedTitleIds = new HashSet<>(titleRecommendationMapper.findAllMatchedTitleIds());
            Set<String> matchedTodayTitleIds = new HashSet<>(titleRecommendationMapper.findMatchedTitleIdsByDate(targetDate));
            List<TitleLibrary> unmatchedTitles = allTitles.stream()
                    .filter(t -> !allMatchedTitleIds.contains(t.getId())) // 过滤掉已绑定用户的标题
                    .filter(t -> t.getIsUsed() == null || t.getIsUsed() != 1) // 过滤掉 is_used=1 的标题
                    .collect(Collectors.toList());

            // 在未匹配的标题中，优先匹配 pushDate 等于目标日期的；pushDate 为 null 的也允许匹配
            // 同时过滤掉没有 trackId 的标题（无法匹配任何用户）
            List<TitleLibrary> titles = unmatchedTitles.stream()
                    .filter(t -> t.getPushDate() == null || !t.getPushDate().isAfter(targetDate))
                    .filter(t -> t.getTrackId() != null && !t.getTrackId().isEmpty())
                    .collect(Collectors.toList());

            List<User> users = userMapper.findAll().stream()
                    .filter(u -> u.getStatus() != null && u.getStatus() == 1)
                    .filter(u -> u.getIsDeleted() == null || u.getIsDeleted() != 1)
                    .filter(u -> u.getUserType() != null && u.getUserType() >= 1 && u.getUserType() <= 3)
                    .collect(Collectors.toList());

            // ===== 严格过滤2：目标日期同一用户同一赛道已有推荐的，不再重复匹配（不同赛道可匹配多个） =====
            Set<String> existingCombos = new HashSet<>();
            for (Map<String, Object> row : titleRecommendationMapper.findUserTrackCombosByDate(targetDate)) {
                String uid = (String) row.get("user_id");
                String tid = (String) row.get("track_id");
                if (uid != null && tid != null) {
                    existingCombos.add(uid + ":" + tid);
                }
            }

            // Build user active track subscriptions map (only non-frozen tracks)
            Map<String, Set<String>> userTrackMap = new HashMap<>();
            for (User user : users) {
                Set<String> activeTrackIds = getActiveTrackIdsForUser(user);
                userTrackMap.put(user.getId(), activeTrackIds);
            }

            // Build trackId -> trackName map for title-name fallback matching
            List<Track> allTracks = trackMapper.findAll();
            Map<String, String> trackNameMap = new HashMap<>();
            for (Track t : allTracks) {
                if (t.getName() != null) {
                    trackNameMap.put(t.getId(), t.getName());
                }
            }

            // ===== 过滤3：加载每个用户历史上绑定过的标题ID集合 =====
            Map<String, Set<String>> userHistoryMap = new HashMap<>();
            for (User user : users) {
                List<String> historyIds = titleRecommendationMapper.findHistoricallyMatchedTitleIdsByUserId(user.getId());
                userHistoryMap.put(user.getId(), new HashSet<>(historyIds));
            }

            int matched = 0;
            int skipped = 0;
            int alreadyMatchedToday = matchedTodayTitleIds.size();
            Random random = new Random();
            List<Map<String, Object>> skipReasons = new ArrayList<>();
            List<String> matchedTitleIds = new ArrayList<>();
            List<String> matchedTitleNames = new ArrayList<>();

            // ===== 过滤2：本次调用内，同一用户同一赛道已匹配过的不再重复匹配 =====
            Set<String> matchedCombosThisRun = new HashSet<>();

            // 追踪每个用户每个赛道的未匹配原因（用于诊断）
            Map<String, Map<String, Integer>> userTrackSkipReasons = new HashMap<>();

            System.out.println("[matchToday] 目标日期=" + targetDate
                    + ", 总标题=" + allTitles.size()
                    + ", 当天已匹配=" + alreadyMatchedToday
                    + ", 未匹配=" + unmatchedTitles.size()
                    + ", 本次可匹配=" + titles.size()
                    + ", 用户总数=" + users.size()
                    + ", 当天已匹配组合=" + existingCombos.size());

            for (TitleLibrary title : titles) {
                // Find eligible users
                List<User> eligible = new ArrayList<>();
                int skipNoTrack = 0;
                int skipAlreadyMatchedToday = 0;
                int skipAlreadyMatchedThisRun = 0;

                int skipHistoryBound = 0;
                for (User user : users) {
                    String comboKey = user.getId() + ":" + title.getTrackId();
                    String userTrackKey = user.getId() + "-" + title.getTrackId();
                    userTrackSkipReasons.putIfAbsent(userTrackKey, new HashMap<>());
                    Map<String, Integer> reasons = userTrackSkipReasons.get(userTrackKey);

                    // 当天同一用户同一赛道已经匹配过，跳过
                    if (existingCombos.contains(comboKey)) {
                        skipAlreadyMatchedToday++;
                        reasons.put("alreadyMatchedToday", reasons.getOrDefault("alreadyMatchedToday", 0) + 1);
                        continue;
                    }

                    // 本次调用内同一用户同一赛道已经匹配过，跳过
                    if (matchedCombosThisRun.contains(comboKey)) {
                        skipAlreadyMatchedThisRun++;
                        reasons.put("alreadyMatchedThisRun", reasons.getOrDefault("alreadyMatchedThisRun", 0) + 1);
                        continue;
                    }

                    // 历史上该用户已绑定过这个标题，跳过
                    Set<String> historyIds = userHistoryMap.getOrDefault(user.getId(), Collections.emptySet());
                    if (historyIds.contains(title.getId())) {
                        skipHistoryBound++;
                        reasons.put("historyBound", reasons.getOrDefault("historyBound", 0) + 1);
                        continue;
                    }

                    // Check track match (trackId exact match OR title contains track name)
                    boolean trackMatched = false;
                    Set<String> userTracks = userTrackMap.getOrDefault(user.getId(), Collections.emptySet());
                    if (title.getTrackId() != null && !title.getTrackId().isEmpty()) {
                        if (userTracks.contains(title.getTrackId())) {
                            trackMatched = true;
                        }
                    }
                    // Fallback: title name contains any subscribed track name
                    if (!trackMatched && title.getTitle() != null && !title.getTitle().isEmpty()) {
                        for (String utid : userTracks) {
                            String trackName = trackNameMap.get(utid);
                            if (trackName != null && !trackName.isEmpty()
                                    && title.getTitle().contains(trackName)) {
                                trackMatched = true;
                                break;
                            }
                        }
                    }
                    if (!trackMatched) {
                        skipNoTrack++;
                        reasons.put("noTrack", reasons.getOrDefault("noTrack", 0) + 1);
                        continue;
                    }

                    eligible.add(user);
                }

                if (eligible.isEmpty()) {
                    skipped++;
                    Map<String, Object> reason = new HashMap<>();
                    reason.put("title", title.getTitle());
                    reason.put("trackId", title.getTrackId());
                    reason.put("skipNoTrack", skipNoTrack);
                    reason.put("skipAlreadyMatchedToday", skipAlreadyMatchedToday);
                    reason.put("skipAlreadyMatchedThisRun", skipAlreadyMatchedThisRun);
                    reason.put("skipHistoryBound", skipHistoryBound);
                    skipReasons.add(reason);
                    System.out.println("[matchToday] 跳过: " + title.getTitle() + " | 赛道不符=" + skipNoTrack + " 当天已匹配=" + skipAlreadyMatchedToday + " 本次已匹配=" + skipAlreadyMatchedThisRun + " 历史绑定=" + skipHistoryBound);
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
                rec.setRecommendDate(targetDate);
                titleRecommendationMapper.insert(rec);
                titleLibraryService.updatePushDate(title.getId(), targetDate);
                titleLibraryService.updateIsUsed(title.getId(), 1);

                // 标记本次调用内该用户+赛道组合已匹配（不同赛道仍可继续匹配）
                String matchedCombo = selected.getId() + ":" + title.getTrackId();
                matchedCombosThisRun.add(matchedCombo);
                // 同时加入当天已匹配集合（影响后续标题的跳过统计）
                existingCombos.add(matchedCombo);

                matched++;
                matchedTitleIds.add(title.getId());
                matchedTitleNames.add(title.getTitle());
                System.out.println("[matchToday] 匹配: " + title.getTitle() + " -> " + selected.getUsername());
            }

            // 统计有多少用户/赛道组合完全没有被匹配上
            int totalCombos = 0;
            int unmatchedCombos = 0;
            Map<String, Integer> globalReasons = new HashMap<>();
            for (User user : users) {
                Set<String> activeTracks = userTrackMap.getOrDefault(user.getId(), Collections.emptySet());
                for (String trackId : activeTracks) {
                    String comboKey = user.getId() + ":" + trackId;
                    totalCombos++;
                    if (existingCombos.contains(comboKey) || matchedCombosThisRun.contains(comboKey)) {
                        continue; // 已匹配（当天已有或本次已匹配）
                    }
                    unmatchedCombos++;
                    String userTrackKey = user.getId() + "-" + trackId;
                    Map<String, Integer> reasons = userTrackSkipReasons.getOrDefault(userTrackKey, Collections.emptyMap());
                    for (Map.Entry<String, Integer> entry : reasons.entrySet()) {
                        globalReasons.put(entry.getKey(), globalReasons.getOrDefault(entry.getKey(), 0) + entry.getValue());
                    }
                }
            }

            System.out.println("[matchToday] 完成: 匹配=" + matched + ", 跳过=" + skipped + ", 当天已匹配=" + alreadyMatchedToday
                    + ", 总用户赛道组合=" + totalCombos + ", 未匹配组合=" + unmatchedCombos
                    + ", 当天已存在组合=" + existingCombos.size()
                    + ", 原因统计=" + globalReasons);

            Map<String, Object> result = new HashMap<>();
            result.put("matched", matched);
            result.put("skipped", skipped);
            result.put("alreadyMatchedToday", alreadyMatchedToday);
            result.put("targetDate", targetDate.toString());
            result.put("titleCount", titles.size());
            result.put("userCount", users.size());
            result.put("totalCombos", totalCombos);
            result.put("unmatchedCombos", unmatchedCombos);
            result.put("existingCombosCount", existingCombos.size());
            result.put("unmatchReasons", globalReasons);
            result.put("matchedTitleIds", matchedTitleIds);
            result.put("matchedTitleNames", matchedTitleNames);
            if (!skipReasons.isEmpty()) {
                result.put("skipDetails", skipReasons.subList(0, Math.min(skipReasons.size(), 10)));
            }
            return Result.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("匹配失败：" + e.getMessage());
        }
    }

    /**
     * 匹配预览：模拟匹配逻辑，返回待审核的匹配列表（不保存）
     */
    @GetMapping("/match-preview")
    public Result<List<Map<String, Object>>> matchPreview(@RequestParam(value = "date", required = false) String dateStr) {
        try {
            LocalDate targetDate = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();
            List<TitleLibrary> allTitles = titleLibraryService.list();

            Set<String> allMatchedTitleIds = new HashSet<>(titleRecommendationMapper.findAllMatchedTitleIds());
            List<TitleLibrary> titles = allTitles.stream()
                    .filter(t -> !allMatchedTitleIds.contains(t.getId())) // 过滤掉已绑定用户的标题
                    .filter(t -> t.getIsUsed() == null || t.getIsUsed() != 1) // 过滤掉 is_used=1 的标题
                    .filter(t -> t.getPushDate() == null || !t.getPushDate().isAfter(targetDate))
                    .filter(t -> t.getTrackId() != null && !t.getTrackId().isEmpty())
                    .collect(Collectors.toList());

            List<User> users = userMapper.findAll().stream()
                    .filter(u -> u.getStatus() != null && u.getStatus() == 1)
                    .filter(u -> u.getIsDeleted() == null || u.getIsDeleted() != 1)
                    .filter(u -> u.getUserType() != null && u.getUserType() >= 1 && u.getUserType() <= 3)
                    .collect(Collectors.toList());

            Set<String> existingCombos = new HashSet<>();
            for (Map<String, Object> row : titleRecommendationMapper.findUserTrackCombosByDate(targetDate)) {
                String uid = (String) row.get("user_id");
                String tid = (String) row.get("track_id");
                if (uid != null && tid != null) existingCombos.add(uid + ":" + tid);
            }

            Map<String, Set<String>> userTrackMap = new HashMap<>();
            for (User user : users) {
                userTrackMap.put(user.getId(), getActiveTrackIdsForUser(user));
            }

            List<Track> allTracks = trackMapper.findAll();
            Map<String, String> trackNameMap = new HashMap<>();
            for (Track t : allTracks) {
                if (t.getName() != null) trackNameMap.put(t.getId(), t.getName());
            }

            Map<String, Set<String>> userHistoryMap = new HashMap<>();
            for (User user : users) {
                List<String> historyIds = titleRecommendationMapper.findHistoricallyMatchedTitleIdsByUserId(user.getId());
                userHistoryMap.put(user.getId(), new HashSet<>(historyIds));
            }

            Set<String> matchedCombosThisRun = new HashSet<>();
            Set<String> matchedTitleIdsThisRun = new HashSet<>();
            Random random = new Random();
            List<Map<String, Object>> proposedMatches = new ArrayList<>();

            // 按赛道分组收集所有候选匹配对，再统一打乱随机抽取
            List<Map<String, Object>> allCandidates = new ArrayList<>();
            for (TitleLibrary title : titles) {
                if (matchedTitleIdsThisRun.contains(title.getId())) continue;
                for (User user : users) {
                    String comboKey = user.getId() + ":" + title.getTrackId();
                    if (existingCombos.contains(comboKey) || matchedCombosThisRun.contains(comboKey)) continue;
                    Set<String> historyIds = userHistoryMap.getOrDefault(user.getId(), Collections.emptySet());
                    if (historyIds.contains(title.getId())) continue;
                    Set<String> userTracks = userTrackMap.getOrDefault(user.getId(), Collections.emptySet());
                    boolean trackMatched = userTracks.contains(title.getTrackId());
                    if (!trackMatched && title.getTitle() != null) {
                        for (String utid : userTracks) {
                            String trackName = trackNameMap.get(utid);
                            if (trackName != null && title.getTitle().contains(trackName)) {
                                trackMatched = true;
                                break;
                            }
                        }
                    }
                    if (!trackMatched) continue;

                    Map<String, Object> cand = new HashMap<>();
                    cand.put("title", title);
                    cand.put("user", user);
                    allCandidates.add(cand);
                }
            }

            // 打乱所有候选匹配对
            Collections.shuffle(allCandidates, random);

            Set<String> usedTitleIds = new HashSet<>();
            Set<String> usedCombos = new HashSet<>();
            for (Map<String, Object> cand : allCandidates) {
                TitleLibrary title = (TitleLibrary) cand.get("title");
                User user = (User) cand.get("user");

                if (usedTitleIds.contains(title.getId())) continue;
                String combo = user.getId() + ":" + title.getTrackId();
                if (usedCombos.contains(combo)) continue;

                usedTitleIds.add(title.getId());
                usedCombos.add(combo);

                Map<String, Object> item = new HashMap<>();
                item.put("titleId", title.getId());
                item.put("title", title.getTitle());
                item.put("platform", title.getPlatform());
                item.put("trackId", title.getTrackId());
                item.put("trackName", trackNameMap.getOrDefault(title.getTrackId(), title.getTrackId()));
                item.put("userId", user.getId());
                item.put("username", user.getUsername());
                item.put("userEmail", user.getEmail());
                item.put("editedTitle", title.getTitle());
                proposedMatches.add(item);
            }

            return Result.ok(proposedMatches);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("预览失败：" + e.getMessage());
        }
    }

    /**
     * 确认匹配：保存审核后批准的部分匹配项
     */
    @PostMapping("/match-confirm")
    public Result<Map<String, Object>> matchConfirm(
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestBody List<Map<String, Object>> matches) {
        try {
            LocalDate targetDate = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();
            int saved = 0;
            List<String> savedIds = new ArrayList<>();
            for (Map<String, Object> m : matches) {
                String titleId = m.get("titleId") != null ? m.get("titleId").toString() : null;
                String userId = m.get("userId") != null ? m.get("userId").toString() : null;
                String editedTitle = m.get("editedTitle") != null ? m.get("editedTitle").toString() : null;
                if (titleId == null || userId == null) continue;

                TitleLibrary title = titleLibraryService.getById(titleId);
                if (title == null) continue;

                // 如果编辑了标题，先更新标题
                if (editedTitle != null && !editedTitle.equals(title.getTitle())) {
                    title.setTitle(editedTitle);
                    titleLibraryService.updateTitle(title);
                }

                TitleRecommendation rec = new TitleRecommendation();
                rec.setId(UUID.randomUUID().toString().replace("-", ""));
                rec.setTitleLibraryId(titleId);
                rec.setUserId(userId);
                rec.setPlatform(title.getPlatform());
                rec.setTrackId(title.getTrackId());
                rec.setRecommendDate(targetDate);
                titleRecommendationMapper.insert(rec);
                titleLibraryService.updatePushDate(titleId, targetDate);
                titleLibraryService.updateIsUsed(titleId, 1);
                saved++;
                savedIds.add(titleId);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("saved", saved);
            result.put("savedTitleIds", savedIds);
            return Result.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("确认匹配失败：" + e.getMessage());
        }
    }

    /**
     * 重新匹配单个标题：针对指定用户获取另一个可用的匹配标题
     */
    @GetMapping("/match-one")
    public Result<Map<String, Object>> matchOne(
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "titleId", required = false) String currentTitleId) {
        try {
            LocalDate targetDate = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();

            if (userId == null || userId.isEmpty()) {
                return Result.error("用户ID不能为空");
            }

            User user = userMapper.findById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            // 获取用户当前已匹配的标题（当天）
            Set<String> matchedTitleIds = new HashSet<>();
            List<Map<String, Object>> existingTitles = titleRecommendationMapper.findMatchedTitlesByDateAndUser(targetDate, userId);
            for (Map<String, Object> row : existingTitles) {
                matchedTitleIds.add((String) row.get("titleId"));
            }
            // 排除当前正在编辑的标题（如果传了的话）
            if (currentTitleId != null && !currentTitleId.isEmpty()) {
                matchedTitleIds.add(currentTitleId);
            }

            // 获取用户可选的赛道
            Set<String> userTrackIds = getActiveTrackIdsForUser(user);

            // 获取该用户历史上已绑定过的标题（排除重复推荐）
            Set<String> historyTitleIds = new HashSet<>();
            List<String> hist = titleRecommendationMapper.findHistoricallyMatchedTitleIdsByUserId(userId);
            historyTitleIds.addAll(hist);

            List<Track> allTracks = trackMapper.findAll();
            Map<String, String> trackNameMap = new HashMap<>();
            for (Track t : allTracks) {
                if (t.getName() != null) trackNameMap.put(t.getId(), t.getName());
            }

            // 找出该用户已占用的 (titleId, trackId) 组合（同一天）
            Set<String> usedCombos = new HashSet<>();
            List<Map<String, Object>> usedComboRows = titleRecommendationMapper.findUserTrackCombosByDate(targetDate);
            for (Map<String, Object> row : usedComboRows) {
                String uid = (String) row.get("user_id");
                if (userId.equals(uid)) {
                    String tid = (String) row.get("track_id");
                    if (tid != null) usedCombos.add(tid);
                }
            }

            // 查所有未匹配的标题
            List<TitleLibrary> allTitles = titleLibraryService.getAllUnmatchedTitles();

            List<TitleLibrary> eligible = new ArrayList<>();
            for (TitleLibrary t : allTitles) {
                String tid = t.getId();
                if (matchedTitleIds.contains(tid)) continue;
                if (historyTitleIds.contains(tid)) continue;
                if (t.getIsUsed() != null && t.getIsUsed() == 1) continue; // 过滤已使用的标题

                String titleTrackId = t.getTrackId();
                boolean trackMatched = userTrackIds.contains(titleTrackId);
                if (!trackMatched && t.getTitle() != null) {
                    for (String utid : userTrackIds) {
                        String trackName = trackNameMap.get(utid);
                        if (trackName != null && t.getTitle().contains(trackName)) {
                            trackMatched = true;
                            break;
                        }
                    }
                }
                if (!trackMatched) continue;

                // 检查赛道是否已被该用户占用（当天）
                if (usedCombos.contains(titleTrackId)) continue;

                eligible.add(t);
            }

            if (eligible.isEmpty()) {
                return Result.error("没有可用的匹配标题");
            }

            Random random = new Random();
            TitleLibrary selected = eligible.get(random.nextInt(eligible.size()));
            Map<String, Object> result = new HashMap<>();
            result.put("titleId", selected.getId());
            result.put("title", selected.getTitle());
            result.put("trackId", selected.getTrackId());
            result.put("platform", selected.getPlatform());
            result.put("trackName", trackNameMap.get(selected.getTrackId()));
            result.put("userId", userId);
            result.put("username", user.getUsername());
            result.put("userEmail", user.getEmail());
            return Result.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("重配失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/used")
    public Result<Void> markUsed(@PathVariable String id) {
        TitleLibrary tl = titleLibraryService.getById(id);
        if (tl == null) {
            return Result.error("标题不存在");
        }
        int newVal = (tl.getIsUsed() != null && tl.getIsUsed() == 1) ? 0 : 1;
        titleLibraryService.updateIsUsed(id, newVal);
        return Result.ok(null);
    }

    /** 临时接口：把所有已有关联推荐记录的标题标记为已使用 */
    @PostMapping("/batch-mark-used-for-matched")
    public Result<Map<String, Object>> batchMarkUsedForMatched() {
        int updated = titleLibraryService.batchMarkUsedForMatched();
        Map<String, Object> result = new HashMap<>();
        result.put("updated", updated);
        return Result.ok(result);
    }

    @DeleteMapping("/{id}/recommendation")
    public Result<Void> unbindRecommendation(@PathVariable String id) {
        titleRecommendationMapper.deleteByTitleId(id);
        titleLibraryService.updatePushDate(id, null);
        titleLibraryService.updateIsUsed(id, 0);
        return Result.ok(null);
    }

    @PostMapping("/batch-unbind")
    public Result<Map<String, Object>> batchUnbindRecommendations(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> titleIds = (List<String>) body.get("titleIds");
        if (titleIds == null || titleIds.isEmpty()) {
            return Result.error("请选择要解绑的标题");
        }
        int success = 0;
        int failed = 0;
        for (String id : titleIds) {
            try {
                titleRecommendationMapper.deleteByTitleId(id);
                titleLibraryService.updatePushDate(id, null);
                titleLibraryService.updateIsUsed(id, 0);
                success++;
            } catch (Exception e) {
                failed++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        return Result.ok(result);
    }

    @PostMapping("/batch-ai-passed")
    public Result<Map<String, Object>> batchAiPassed(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> titleIds = (List<String>) body.get("titleIds");
        if (titleIds == null || titleIds.isEmpty()) {
            return Result.error("请选择要标记的标题");
        }
        int success = 0;
        int failed = 0;
        for (String id : titleIds) {
            try {
                titleLibraryService.updateIsAiPassed(id, 1);
                success++;
            } catch (Exception e) {
                failed++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        return Result.ok(result);
    }

    @PostMapping("/batch-copied")
    public Result<Map<String, Object>> batchCopied(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> titleIds = (List<String>) body.get("titleIds");
        if (titleIds == null || titleIds.isEmpty()) {
            return Result.error("请选择要标记的标题");
        }
        int success = 0;
        int failed = 0;
        for (String id : titleIds) {
            try {
                titleLibraryService.updateIsCopied(id, 1);
                success++;
            } catch (Exception e) {
                failed++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        return Result.ok(result);
    }

    /**
     * 查询指定用户历史上绑定的所有标题记录（带相似度）
     */
    @GetMapping("/user-history/{userId}")
    public Result<List<Map<String, Object>>> getUserHistory(
            @PathVariable String userId,
            @RequestParam(value = "title", required = false) String title) {
        try {
            List<Map<String, Object>> list = titleRecommendationMapper.findHistoryByUserId(userId);
            if (list == null) list = new ArrayList<>();
            if (title != null && !title.trim().isEmpty()) {
                for (Map<String, Object> item : list) {
                    String historyTitle = item.get("titleName") != null ? item.get("titleName").toString() : "";
                    double sim = com.example.blogger.util.TextSimilarityUtil.similarity(title, historyTitle);
                    item.put("similarity", (int) Math.round(sim * 100));
                }
                // 按相似度降序
                list.sort((a, b) -> {
                    int simA = ((Number) a.getOrDefault("similarity", 0)).intValue();
                    int simB = ((Number) b.getOrDefault("similarity", 0)).intValue();
                    return Integer.compare(simB, simA);
                });
            }
            return Result.ok(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 清理指定日期的所有推荐记录（用于重新匹配）
     */
    @PostMapping("/clear-recommendations")
    public Result<Map<String, Object>> clearRecommendationsByDate(@RequestParam(value = "date", required = false) String dateStr) {
        try {
            LocalDate targetDate = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : LocalDate.now();

            // 1. 查询目标日期关联的所有标题ID
            List<String> titleIds = titleRecommendationMapper.findMatchedTitleIdsByDate(targetDate);

            // 2. 删除目标日期的推荐记录
            int deleted = titleRecommendationMapper.deleteByDate(targetDate);

            // 3. 清理标题状态：删除后没有其他推荐记录的标题，清空 push_date 和 is_used
            int cleared = 0;
            for (String titleId : titleIds) {
                int remaining = titleRecommendationMapper.countByTitleId(titleId);
                if (remaining == 0) {
                    titleLibraryService.updatePushDate(titleId, null);
                    titleLibraryService.updateIsUsed(titleId, 0);
                    cleared++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("targetDate", targetDate.toString());
            result.put("deletedRecommendations", deleted);
            result.put("affectedTitles", titleIds.size());
            result.put("clearedTitles", cleared);
            return Result.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("清理失败：" + e.getMessage());
        }
    }

    /**
     * 去除AI味：调用本地 python 脚本处理指定路径的文件
     */
    @PostMapping("/remove-ai-flavor")
    public Result<Map<String, Object>> removeAiFlavor(@RequestBody Map<String, Object> params) {
        String path = params.get("path") != null ? params.get("path").toString() : "";
        if (path.isEmpty()) {
            return Result.error("路径不能为空");
        }
        if (replacePeriodsScriptPath == null || replacePeriodsScriptPath.isEmpty()) {
            log.error("[removeAiFlavor] 脚本路径未配置，请检查 app.script.replace-periods-path");
            return Result.error("脚本路径未配置，请联系管理员");
        }
        File scriptFile = new File(replacePeriodsScriptPath);
        if (!scriptFile.exists()) {
            log.error("[removeAiFlavor] 脚本文件不存在: {}", replacePeriodsScriptPath);
            return Result.error("脚本文件不存在: " + replacePeriodsScriptPath);
        }

        try {
            // 构建命令：python script.py <path> [--custom-rules <json>]
            List<String> command = new ArrayList<>();
            command.add("python");
            command.add(replacePeriodsScriptPath);
            command.add(path);

            // 处理自定义规则
            Object rulesObj = params.get("rules");
            if (rulesObj != null) {
                String rulesJson = null;
                if (rulesObj instanceof List) {
                    rulesJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(rulesObj);
                } else if (rulesObj instanceof String) {
                    rulesJson = (String) rulesObj;
                }
                if (rulesJson != null && !rulesJson.isEmpty()) {
                    command.add("--custom-rules");
                    command.add(rulesJson);
                }
            }

            log.info("[removeAiFlavor] 执行脚本: {}", command);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取 stdout
            StringBuilder stdout = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return Result.error("脚本执行超时（30秒）");
            }

            int exitCode = process.exitValue();
            Map<String, Object> result = new HashMap<>();
            result.put("exitCode", exitCode);
            result.put("stdout", stdout.toString().trim());
            log.info("[removeAiFlavor] 脚本执行完成 exitCode={}", exitCode);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("[removeAiFlavor] 执行失败", e);
            return Result.error("执行失败：" + e.getMessage());
        }
    }

    /**
     * 自动插入图片：调用本地 python 脚本处理指定目录的文档
     */
    @PostMapping("/auto-insert-images")
    public Result<Map<String, Object>> autoInsertImages(@RequestBody Map<String, Object> params) {
        String fileDir = params.get("fileDir") != null ? params.get("fileDir").toString() : "";
        String imageLibDir = params.get("imageLibDir") != null ? params.get("imageLibDir").toString() : "";
        int count = 1;
        if (params.get("count") != null) {
            try { count = Integer.parseInt(params.get("count").toString()); } catch (Exception ignored) {}
        }
        if (fileDir.isEmpty()) return Result.error("文件目录不能为空");
        if (imageLibDir.isEmpty()) return Result.error("图片库目录不能为空");
        if (count < 1) return Result.error("图片数量必须 >= 1");

        if (autoInsertImagesScriptPath == null || autoInsertImagesScriptPath.isEmpty()) {
            log.error("[autoInsertImages] 脚本路径未配置，请检查 app.script.auto-insert-images-path");
            return Result.error("脚本路径未配置，请联系管理员");
        }
        File scriptFile = new File(autoInsertImagesScriptPath);
        if (!scriptFile.exists()) {
            log.error("[autoInsertImages] 脚本文件不存在: {}", autoInsertImagesScriptPath);
            return Result.error("脚本文件不存在: " + autoInsertImagesScriptPath);
        }

        try {
            log.info("[autoInsertImages] 执行脚本: python {} {} {} {}", autoInsertImagesScriptPath, fileDir, imageLibDir, count);
            ProcessBuilder pb = new ProcessBuilder("python", autoInsertImagesScriptPath, fileDir, imageLibDir, String.valueOf(count));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder stdout = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) stdout.append(line).append("\n");
            }
            process.waitFor();

            int exitCode = process.exitValue();
            Map<String, Object> result = new HashMap<>();
            result.put("exitCode", exitCode);
            result.put("stdout", stdout.toString().trim());
            log.info("[autoInsertImages] 脚本执行完成 exitCode={}", exitCode);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("[autoInsertImages] 执行失败", e);
            return Result.error("执行失败：" + e.getMessage());
        }
    }

    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestBody Map<String, Object> params) {
        int countPerCombo = params.get("countPerCombo") != null ?
                Integer.parseInt(params.get("countPerCombo").toString()) : 3;
        String rawOutputPath = params.get("outputPath") != null ?
                params.get("outputPath").toString() : "";
        String instruction = params.get("instruction") != null ?
                params.get("instruction").toString() : "";

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

        executor.submit(() -> runGenerateTask(taskId, countPerCombo, outputPath, selectedPlatforms, selectedTrackIds, instruction));

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

    // ==================== 文章反馈相关 ====================

    /**
     * 保存文章反馈（针对某个赛道）
     */
    @PostMapping("/feedback")
    public Result<Void> saveFeedback(@RequestBody ArticleFeedback feedback) {
        if (feedback.getContent() == null || feedback.getContent().trim().isEmpty()) {
            return Result.error("反馈内容不能为空");
        }
        if (feedback.getId() == null || feedback.getId().isEmpty()) {
            feedback.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        articleFeedbackMapper.insert(feedback);
        return Result.ok(null);
    }

    /**
     * 查询某个赛道的文章反馈列表
     */
    @GetMapping("/feedback")
    public Result<List<ArticleFeedback>> listFeedback(
            @RequestParam(value = "trackId", required = false) String trackId,
            @RequestParam(value = "platform", required = false) String platform) {
        return Result.ok(articleFeedbackMapper.findByTrackAndPlatform(trackId, platform));
    }

    /**
     * 删除文章反馈
     */
    @DeleteMapping("/feedback/{id}")
    public Result<Void> deleteFeedback(@PathVariable String id) {
        articleFeedbackMapper.deleteById(id);
        return Result.ok(null);
    }

    // ==================== 单标题生成文章 ====================

    /**
     * 测试 Kimi API 连通性（直接调用 LLMService，不创建任务）
     */
    @GetMapping("/test-kimi")
    public Result<Map<String, Object>> testKimiApi(@RequestParam(value = "prompt", defaultValue = "hello") String prompt) {
        try {
            String content = llmService.generateContent(prompt);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("contentLength", content.length());
            result.put("contentPreview", content.substring(0, Math.min(200, content.length())));
            return Result.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return Result.ok(result);
        }
    }

    // /**
    //  * 为指定标题生成文章（直接调用配置的大模型生成）—— 已废弃，请使用 create-generation-task
    //  */
    // @PostMapping("/{id}/generate-post")
    // public Result<Map<String, Object>> generatePostSingle(@PathVariable String id) {
    //     TitleLibrary titleLib = titleLibraryService.getById(id);
    //     if (titleLib == null) {
    //         return Result.error("标题不存在");
    //     }
    //     try {
    //         // Step 1: Build prompt from copy-prompt template (system auto-get)
    //         String prompt = buildPromptFromTemplate(titleLib);
    //
    //         // Step 2: Call LLM to generate content
    //         log.info("[GeneratePost] Calling LLM for title: {}", titleLib.getTitle());
    //         String content = llmService.generateContent(prompt);
    //         log.info("[GeneratePost] LLM returned content length: {}", content.length());
    //
    //         // Step 3: Generate DOCX file (文件名使用标题名)
    //         String safeTitle = titleLib.getTitle() != null ? titleLib.getTitle() : "untitled";
    //         safeTitle = safeTitle.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", "").trim();
    //         safeTitle = safeTitle.replaceAll("\\s+", "，");
    //         if (safeTitle.isEmpty()) {
    //             safeTitle = "article_" + id;
    //         }
    //         String fileName = safeTitle + ".docx";
    //         String articlesDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "articles";
    //         File dir = new File(articlesDir);
    //         if (!dir.exists()) {
    //             dir.mkdirs();
    //         }
    //         String filePath = articlesDir + File.separator + fileName;
    //         log.info("[GeneratePost] Generating DOCX: {}", filePath);
    //         docxGenerator.generateDocx(titleLib.getTitle(), content, filePath);
    //         log.info("[GeneratePost] DOCX generated successfully");
    //
    //         // Step 4: Update record
    //         String fileUrl = "/uploads/articles/" + fileName;
    //         log.info("[GeneratePost] Updating DB record: id={}, fileUrl={}, fileName={}", id, fileUrl, fileName);
    //         titleLibraryService.updateGeneratedFile(id, fileUrl, fileName);
    //         log.info("[GeneratePost] DB updated successfully");
    //
    //         Map<String, Object> result = new HashMap<>();
    //         result.put("fileUrl", fileUrl);
    //         result.put("fileName", fileName);
    //         result.put("title", titleLib.getTitle());
    //         return Result.ok(result);
    //     } catch (Exception e) {
    //         log.error("[GeneratePost] Failed: {}", e.getMessage(), e);
    //         return Result.error("生成文章失败: " + e.getMessage());
    //     }
    // }

    /**
     * 为指定标题创建异步生成任务（插入任务表，由定时任务消费）
     */
    @PostMapping("/{id}/create-generation-task")
    public Result<Map<String, Object>> createGenerationTask(@PathVariable String id) {
        TitleLibrary titleLib = titleLibraryService.getById(id);
        if (titleLib == null) {
            return Result.error("标题不存在");
        }
        try {
            // Step 1: Build prompt from PromptTemplate (strictly user-defined template)
            String prompt = buildPromptFromPromptTemplate(titleLib);
            log.info("[CreateTask] Built prompt for title: {}, prompt length: {}", titleLib.getTitle(), prompt.length());

            // Step 2: Create task record (允许重复提交，生成新的任务)
            com.example.blogger.entity.TitleGenerationTask task = titleGenerationTaskService.createTask(id, titleLib.getTitle(), prompt);

            // 标记标题为生成中
            titleLibraryService.updateGenerateStatus(id, 2);

            log.info("[CreateTask] Task created: id={}, titleLibraryId={}, title={}", task.getId(), id, titleLib.getTitle());

            Map<String, Object> result = new HashMap<>();
            result.put("taskId", task.getId());
            result.put("status", "pending");
            result.put("message", "生成任务已创建，系统将在后台自动处理");
            return Result.ok(result);
        } catch (Exception e) {
            log.error("[CreateTask] Failed: {}", e.getMessage(), e);
            return Result.error("创建生成任务失败: " + e.getMessage());
        }
    }

    /**
     * 查询标题生成任务状态
     */
    @GetMapping("/{id}/task-status")
    public Result<Map<String, Object>> getTaskStatus(@PathVariable String id) {
        List<com.example.blogger.entity.TitleGenerationTask> tasks = titleGenerationTaskService.findByTitleLibraryId(id);
        if (tasks.isEmpty()) {
            return Result.ok(Map.of("hasTask", false));
        }
        // Return the latest task
        var latestTask = tasks.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("hasTask", true);
        result.put("taskId", latestTask.getId());
        result.put("status", latestTask.getStatus());
        result.put("fileUrl", latestTask.getResultFileUrl());
        result.put("fileName", latestTask.getResultFileName());
        result.put("errorMessage", latestTask.getErrorMessage());
        result.put("createdAt", latestTask.getCreatedAt());
        result.put("progressStep", latestTask.getProgressStep());
        result.put("progressMessage", latestTask.getProgressMessage());
        return Result.ok(result);
    }

    /**
     * 上传生成的文章文件（由本地环境调用）
     */
    @PostMapping("/upload-article")
    public Result<Map<String, Object>> uploadArticleFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("taskId") String taskId) {
        try {
            var task = titleGenerationTaskService.findById(taskId);
            if (task == null) {
                return Result.error("任务不存在");
            }

            // Save file to uploads/articles/
            String articlesDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "articles";
            File dir = new File(articlesDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalName = file.getOriginalFilename();
            String fileName = originalName != null ? originalName : ("article_" + taskId + ".docx");
            String filePath = articlesDir + File.separator + fileName;
            file.transferTo(new File(filePath));

            String fileUrl = "/uploads/articles/" + fileName;

            // Update TitleLibrary
            titleLibraryService.updateGeneratedFile(task.getTitleLibraryId(), fileUrl, fileName);

            // Update task
            titleGenerationTaskService.updateStatus(taskId, "completed", fileUrl);

            // Create SubscriptionPost for preview/download
            TitleLibrary titleLib = titleLibraryService.getById(task.getTitleLibraryId());
            if (titleLib != null) {
                SubscriptionPost post = new SubscriptionPost();
                post.setTitleLibraryId(task.getTitleLibraryId());
                post.setTrackId(titleLib.getTrackId());
                post.setTitle(titleLib.getTitle());
                post.setFileUrl(fileUrl);
                post.setFileName(fileName);
                post.setStatus("已上架");
                post.setUsed(0);
                subscriptionPostService.save(post);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("fileUrl", fileUrl);
            result.put("fileName", fileName);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.error("上传文件失败: " + e.getMessage());
        }
    }

    /**
     * 构建单标题文章生成的 prompt（注入赛道反馈）
     */
    private String buildSingleArticlePrompt(TitleLibrary titleLib) {
        String styleDesc = "";
        String styleCss = buildDefaultStyleCss();
        Style defaultStyle = styleMapper.findDefault();
        if (defaultStyle != null && defaultStyle.getStyleJson() != null && !defaultStyle.getStyleJson().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode styleNode = mapper.readTree(defaultStyle.getStyleJson());
                StringBuilder sb = new StringBuilder();
                sb.append("字体：").append(styleNode.path("fontFamily").asText("默认")).append("；");
                sb.append("正文字号：").append(styleNode.path("fontSize").asText("16px")).append("；");
                sb.append("行高：").append(styleNode.path("lineHeight").asText("1.8")).append("；");
                sb.append("段落间距：").append(styleNode.path("paragraphSpacing").asText("1em")).append("；");
                sb.append("标题颜色：").append(styleNode.path("titleColor").asText("#333")).append("；");
                sb.append("正文颜色：").append(styleNode.path("textColor").asText("#333")).append("；");
                styleDesc = sb.toString();
                styleCss = buildStyleCss(styleNode);
            } catch (Exception e) {
                // ignore
            }
        }

        // Load prompt template
        com.example.blogger.entity.PromptTemplate promptTemplate = promptTemplateMapper.findDefaultByType("generate_post");
        if (promptTemplate == null) {
            promptTemplate = promptTemplateMapper.findLatestByType("generate_post");
        }

        String promptText;
        if (promptTemplate != null && promptTemplate.getContent() != null && !promptTemplate.getContent().isEmpty()) {
            String templateContent = promptTemplate.getContent();

            // Build stylePrompt (same as buildPromptFromTemplate)
            String stylePrompt = "";
            if (defaultStyle != null) {
                stylePrompt = defaultStyle.getScene() != null ? defaultStyle.getScene() : "";
            }

            promptText = templateContent
                    // Support both {var} and ${var} formats
                    .replace("{title}", nvl(titleLib.getTitle()))
                    .replace("${title}", nvl(titleLib.getTitle()))
                    .replace("{description}", nvl(titleLib.getDescription()))
                    .replace("${description}", nvl(titleLib.getDescription()))
                    .replace("{styleDesc}", styleDesc)
                    .replace("${styleDesc}", styleDesc)
                    .replace("{styleRef}", "")
                    .replace("${styleRef}", "")
                    .replace("{stylePrompt}", stylePrompt)
                    .replace("${stylePrompt}", stylePrompt)
                    .replace("{platform}", nvl(titleLib.getPlatform()))
                    .replace("${platform}", nvl(titleLib.getPlatform()))
                    .replace("{trackId}", nvl(titleLib.getTrackId()))
                    .replace("${trackId}", nvl(titleLib.getTrackId()))
                    .replace("{useCount}", titleLib.getUseCount() != null ? titleLib.getUseCount().toString() : "")
                    .replace("${useCount}", titleLib.getUseCount() != null ? titleLib.getUseCount().toString() : "")
                    .replace("{isUsed}", titleLib.getIsUsed() != null ? titleLib.getIsUsed().toString() : "")
                    .replace("${isUsed}", titleLib.getIsUsed() != null ? titleLib.getIsUsed().toString() : "")
                    .replace("{pushDate}", titleLib.getPushDate() != null ? titleLib.getPushDate().toString() : "")
                    .replace("${pushDate}", titleLib.getPushDate() != null ? titleLib.getPushDate().toString() : "")
                    .replace("{recommendUserName}", nvl(titleLib.getRecommendUserName()))
                    .replace("${recommendUserName}", nvl(titleLib.getRecommendUserName()));
        } else {
            StringBuilder prompt = new StringBuilder();
            prompt.append("请根据以下标题和描述，生成一篇完整的公众号风格文章。\n\n");
            prompt.append("标题：").append(titleLib.getTitle()).append("\n");
            prompt.append("描述：").append(titleLib.getDescription() != null ? titleLib.getDescription() : "").append("\n\n");
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

        // Inject track feedback
        String feedback = loadTrackFeedback(titleLib.getTrackId(), titleLib.getPlatform());
        if (feedback != null && !feedback.isEmpty()) {
            promptText += "\n\n【历史反馈 - 生成时需避免】\n" + feedback;
        }

        return promptText;
    }

    /**
     * 使用 rowPromptTemplate 模板填充变量后构建 prompt
     */
    private String buildPromptFromTemplate(TitleLibrary titleLib) {
        // Read row prompt template from config
        String template = getRowPromptTemplate();
        if (template == null || template.isEmpty()) {
            // Fallback: use the old buildSingleArticlePrompt
            return buildSingleArticlePrompt(titleLib);
        }

        String result = template;

        // Inject stylePrompt
        String stylePrompt = "";
        Style defaultStyle = styleMapper.findDefault();
        if (defaultStyle != null) {
            stylePrompt = defaultStyle.getScene() != null ? defaultStyle.getScene() : "";
        }
        result = result.replace("${stylePrompt}", stylePrompt);

        // Inject field variables
        result = result.replace("${title}", nvl(titleLib.getTitle()));
        result = result.replace("${description}", nvl(titleLib.getDescription()));
        result = result.replace("${platform}", nvl(titleLib.getPlatform()));
        result = result.replace("${trackId}", nvl(titleLib.getTrackId()));
        result = result.replace("${useCount}", titleLib.getUseCount() != null ? titleLib.getUseCount().toString() : "");
        result = result.replace("${isUsed}", titleLib.getIsUsed() != null ? titleLib.getIsUsed().toString() : "");
        result = result.replace("${pushDate}", titleLib.getPushDate() != null ? titleLib.getPushDate().toString() : "");
        result = result.replace("${recommendUserName}", nvl(titleLib.getRecommendUserName()));

        // Append track feedback (same as old method)
        String feedback = loadTrackFeedback(titleLib.getTrackId(), titleLib.getPlatform());
        if (feedback != null && !feedback.isEmpty()) {
            result += "\n\n【历史反馈 - 生成时需避免】\n" + feedback;
        }

        return result;
    }

    /**
     * 从 PromptTemplate 表读取 row_prompt 模板并替换变量
     * 严格使用用户配置的模板，不擅自拟定提示词
     */
    private String buildPromptFromPromptTemplate(TitleLibrary titleLib) {
        com.example.blogger.entity.PromptTemplate promptTemplate = promptTemplateMapper.findDefaultByType("row_prompt");
        if (promptTemplate == null) {
            promptTemplate = promptTemplateMapper.findLatestByType("row_prompt");
        }

        if (promptTemplate == null || promptTemplate.getContent() == null || promptTemplate.getContent().isEmpty()) {
            throw new RuntimeException("未配置 row_prompt 类型的提示词模板，请先在提示词模板页面配置");
        }

        String templateContent = promptTemplate.getContent();

        // Build styleDesc and stylePrompt from default style
        String styleDesc = "";
        String stylePrompt = "";
        Style defaultStyle = styleMapper.findDefault();
        if (defaultStyle != null) {
            stylePrompt = defaultStyle.getScene() != null ? defaultStyle.getScene() : "";
            if (defaultStyle.getStyleJson() != null && !defaultStyle.getStyleJson().isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode styleNode = mapper.readTree(defaultStyle.getStyleJson());
                    StringBuilder sb = new StringBuilder();
                    sb.append("字体：").append(styleNode.path("fontFamily").asText("默认")).append("；");
                    sb.append("正文字号：").append(styleNode.path("fontSize").asText("16px")).append("；");
                    sb.append("行高：").append(styleNode.path("lineHeight").asText("1.8")).append("；");
                    sb.append("段落间距：").append(styleNode.path("paragraphSpacing").asText("1em")).append("；");
                    sb.append("标题颜色：").append(styleNode.path("titleColor").asText("#333")).append("；");
                    sb.append("正文颜色：").append(styleNode.path("textColor").asText("#333")).append("；");
                    styleDesc = sb.toString();
                } catch (Exception e) {
                    // ignore parse error
                }
            }
        }

        // Replace variables: support both {var} and ${var} formats
        // If variable not present in template, replace() does nothing (keeps original text)
        String result = templateContent
                .replace("{title}", nvl(titleLib.getTitle()))
                .replace("${title}", nvl(titleLib.getTitle()))
                .replace("{description}", nvl(titleLib.getDescription()))
                .replace("${description}", nvl(titleLib.getDescription()))
                .replace("{styleDesc}", styleDesc)
                .replace("${styleDesc}", styleDesc)
                .replace("{styleRef}", "")
                .replace("${styleRef}", "")
                .replace("{stylePrompt}", stylePrompt)
                .replace("${stylePrompt}", stylePrompt)
                .replace("{platform}", nvl(titleLib.getPlatform()))
                .replace("${platform}", nvl(titleLib.getPlatform()))
                .replace("{trackId}", nvl(titleLib.getTrackId()))
                .replace("${trackId}", nvl(titleLib.getTrackId()))
                .replace("{useCount}", titleLib.getUseCount() != null ? titleLib.getUseCount().toString() : "")
                .replace("${useCount}", titleLib.getUseCount() != null ? titleLib.getUseCount().toString() : "")
                .replace("{isUsed}", titleLib.getIsUsed() != null ? titleLib.getIsUsed().toString() : "")
                .replace("${isUsed}", titleLib.getIsUsed() != null ? titleLib.getIsUsed().toString() : "")
                .replace("{pushDate}", titleLib.getPushDate() != null ? titleLib.getPushDate().toString() : "")
                .replace("${pushDate}", titleLib.getPushDate() != null ? titleLib.getPushDate().toString() : "")
                .replace("{recommendUserName}", nvl(titleLib.getRecommendUserName()))
                .replace("${recommendUserName}", nvl(titleLib.getRecommendUserName()));

        // Inject track feedback
        String feedback = loadTrackFeedback(titleLib.getTrackId(), titleLib.getPlatform());
        if (feedback != null && !feedback.isEmpty()) {
            result += "\n\n【历史反馈 - 生成时需避免】\n" + feedback;
        }

        return result;
    }

    private String getRowPromptTemplate() {
        List<Config> configs = configMapper.findAll();
        for (Config c : configs) {
            if ("row_prompt_template".equals(c.getConfigKey())) {
                return c.getConfigValue();
            }
        }
        return "";
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    /**
     * 加载指定赛道的反馈内容（多条合并）
     */
    private String loadTrackFeedback(String trackId, String platform) {
        List<ArticleFeedback> feedbacks = articleFeedbackMapper.findByTrackAndPlatform(trackId, platform);
        if (feedbacks == null || feedbacks.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < feedbacks.size(); i++) {
            sb.append(i + 1).append(". ").append(feedbacks.get(i).getContent().trim());
            if (i < feedbacks.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 获取某标题关联的文章内容（用于预览反馈）
     */
    @GetMapping("/{id}/post-content")
    public Result<Map<String, Object>> getPostContent(@PathVariable String id) {
        TitleLibrary titleLib = titleLibraryService.getById(id);
        if (titleLib == null) {
            return Result.error("标题不存在");
        }
        // Find the latest subscription post for this title
        TitleRecommendation rec = titleRecommendationMapper.findLatestByTitleId(id);
        if (rec == null || rec.getSubscriptionPostId() == null || rec.getSubscriptionPostId().isEmpty()) {
            return Result.error("该标题尚未生成文章");
        }
        SubscriptionPost post = subscriptionPostService.getById(rec.getSubscriptionPostId());
        if (post == null) {
            return Result.error("文章不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("content", post.getDescription());
        result.put("postId", post.getId());
        result.put("trackId", titleLib.getTrackId());
        result.put("platform", titleLib.getPlatform());
        return Result.ok(result);
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

            // 记录推送日志
            EmailPushLog log = new EmailPushLog();
            log.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
            log.setUserId(user.getId());
            log.setPushDate(rec.getRecommendDate() != null ? rec.getRecommendDate() : LocalDate.now());
            log.setType("daily_recommend");
            log.setTitleLibraryId(titleLib.getId());
            emailPushLogMapper.insert(log);

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

    /**
     * 发送文章邮件（带 DOCX 附件）
     */
    @PostMapping("/{id}/send-article-email")
    public Result<Void> sendArticleEmail(@PathVariable String id, @RequestBody Map<String, String> body) {
        TitleLibrary titleLib = titleLibraryService.getById(id);
        if (titleLib == null) {
            return Result.error("标题不存在");
        }
        String fileUrl = titleLib.getGeneratedFileUrl();
        if (fileUrl == null || fileUrl.isEmpty()) {
            return Result.error("该标题尚未生成文章");
        }

        String toEmail = body.get("email");
        if (toEmail == null || toEmail.isEmpty()) {
            return Result.error("邮箱地址不能为空");
        }

        try {
            // 读取文件
            if (!fileUrl.startsWith("/uploads/articles/")) {
                return Result.error("文件路径无效");
            }

            Path basePath = Paths.get(System.getProperty("user.dir"), "uploads", "articles").toAbsolutePath().normalize();
            Path filePath = Paths.get(System.getProperty("user.dir"), fileUrl.replace("/", File.separator)).toAbsolutePath().normalize();

            if (!filePath.startsWith(basePath)) {
                return Result.error("文件路径无效");
            }

            File file = filePath.toFile();
            if (!file.exists()) {
                return Result.error("文章文件不存在");
            }

            // 发送邮件
            String content = buildArticleEmailHtml(titleLib);
            emailService.sendHtmlEmailWithAttachment(toEmail, "您的创作文章", content, file, titleLib.getGeneratedFileName());

            return Result.ok(null);
        } catch (Exception e) {
            return Result.error("发送邮件失败: " + e.getMessage());
        }
    }

    private String buildArticleEmailHtml(TitleLibrary titleLib) {
        String title = HtmlUtils.htmlEscape(titleLib.getTitle());
        return "<html><body><p>您好，您的文章《" + title + "》已生成，附件为 DOCX 文件，请查收。</p></body></html>";
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

                    // 记录推送日志
                    EmailPushLog log = new EmailPushLog();
                    log.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
                    log.setUserId(user.getId());
                    log.setPushDate(rec.getRecommendDate() != null ? rec.getRecommendDate() : LocalDate.now());
                    log.setType("daily_recommend");
                    log.setTitleLibraryId(titleLib.getId());
                    emailPushLogMapper.insert(log);

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
    public Result<List<Map<String, Object>>> listUnrecommendedUsers(
            @RequestParam String date,
            @RequestParam(value = "type", required = false) String type) {
        try {
            LocalDate pushDate = LocalDate.parse(date);
            List<Map<String, Object>> users = titleLibraryService.findUnrecommendedUsers(pushDate, type);
            return Result.ok(users);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/push-overview")
    public Result<Map<String, Object>> listPushOverview(
            @RequestParam String date,
            @RequestParam(value = "type", required = false, defaultValue = "all") String type,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "emailPushed", required = false) String emailPushed,
            @RequestParam(value = "articleComplete", required = false) String articleComplete,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize) {
        try {
            LocalDate pushDate = LocalDate.parse(date);
            Map<String, Object> result = titleLibraryService.findPushOverview(
                    pushDate, type, keyword, emailPushed, articleComplete, page, pageSize);
            return Result.ok(result);
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

            // 幂等性校验：相同日期+用户列表的请求，5分钟内不允许重复提交
            String fingerprint = buildPushFingerprint(dateStr, userIds);
            cleanupExpiredPushFingerprints();
            long expireAt = System.currentTimeMillis() + PUSH_IDEMPOTENCY_TTL_MS;
            Long existing = pushInProgress.putIfAbsent(fingerprint, expireAt);
            if (existing != null && existing > System.currentTimeMillis()) {
                return Result.error("正在推送中，请勿重复提交");
            }

            try {
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

                    // 查找用户当日所有推荐（有关联文章的）
                    List<Map<String, Object>> recMaps = titleRecommendationMapper.findByUserAndDate(userId, pushDate);
                    if (recMaps == null || recMaps.isEmpty()) {
                        failed++;
                        errors.add(Map.of("userId", userId, "reason", "当日没有关联文章的推荐"));
                        continue;
                    }

                    // 查询该用户当日已推送过的 titleLibraryId，避免重复推送
                    List<String> pushedTitleIds = emailPushLogMapper.findPushedTitleIdsByUserAndDate(userId, pushDate);
                    Set<String> pushedSet = new HashSet<>(pushedTitleIds != null ? pushedTitleIds : new ArrayList<>());

                    boolean anyPushed = false;
                    for (Map<String, Object> recMap : recMaps) {
                        String subPostId = recMap.get("subscription_post_id") != null ? recMap.get("subscription_post_id").toString() : null;
                        String trackId = recMap.get("track_id") != null ? recMap.get("track_id").toString() : null;
                        String titleLibId = recMap.get("title_library_id") != null ? recMap.get("title_library_id").toString() : null;
                        // 已推送过的跳过
                        if (titleLibId != null && !titleLibId.isEmpty() && pushedSet.contains(titleLibId)) {
                            continue;
                        }
                        if (subPostId == null || subPostId.isEmpty()) {
                            continue;
                        }
                        SubscriptionPost post = subscriptionPostMapper.findById(subPostId);
                        if (post == null) {
                            continue;
                        }
                        String fileUrl = post.getFileUrl();
                        if (fileUrl == null || fileUrl.isEmpty()) {
                            continue;
                        }
                        File articleFile = new File(fileUrl.startsWith("/")
                                ? System.getProperty("user.dir") + fileUrl
                                : fileUrl);
                        if (!articleFile.exists()) {
                            String articlesDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "articles";
                            articleFile = new File(articlesDir + File.separator + post.getFileName());
                        }
                        if (!articleFile.exists()) {
                            continue;
                        }
                        Track userTrack = trackMapper.findById(trackId);
                        String trackName = userTrack != null ? userTrack.getName() : "";
                        TitleLibrary titleLib = titleLibraryService.getById(titleLibId);
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
                        log.setTitleLibraryId(titleLibId);
                        emailPushLogMapper.insert(log);
                        anyPushed = true;
                    }
                    if (anyPushed) {
                        success++;
                    } else {
                        errors.add(Map.of("user", user.getUsername(), "reason", "所有文章已推送过，无需重复推送"));
                    }
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
            } finally {
                pushInProgress.remove(fingerprint);
            }

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

                // Inject track feedback
                String trackFeedback = loadTrackFeedback(rec.getTrackId(), titleLib.getPlatform());
                if (trackFeedback != null && !trackFeedback.isEmpty()) {
                    promptText += "\n\n【历史反馈 - 生成时需避免】\n" + trackFeedback;
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

            boolean finished = process.waitFor(300, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                log.error("[callClaudeForContent] 命令执行超时(300s)，强制终止进程");
                killClaudeProcess(process);
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
            killClaudeProcess(process);
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
                                 List<String> selectedPlatforms, List<String> selectedTrackIds,
                                 String instruction) {
        Map<String, Object> task = generateTasks.get(taskId);
        log.info("[生成标题] 任务开始 taskId={}, countPerCombo={}, outputPath={}, platforms={}, trackIds={}",
                taskId, countPerCombo, outputPath, selectedPlatforms, selectedTrackIds);
        try {
            List<Track> allTracks = trackMapper.findAll();
            log.info("[生成标题] 查询到赛道数量: {}", allTracks == null ? 0 : allTracks.size());
            if (allTracks == null || allTracks.isEmpty()) {
                task.put("status", "failed");
                task.put("message", "系统中没有赛道数据");
                log.warn("[生成标题] 任务失败: 系统中没有赛道数据");
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
            log.info("[生成标题] 筛选后赛道数量: {}", tracks.size());

            if (tracks.isEmpty()) {
                task.put("status", "failed");
                task.put("message", "选择的赛道中没有可用数据");
                log.warn("[生成标题] 任务失败: 选择的赛道中没有可用数据");
                return;
            }

            List<String> platforms;
            if (selectedPlatforms != null && !selectedPlatforms.isEmpty()) {
                platforms = selectedPlatforms;
            } else {
                platforms = Arrays.asList("公众号", "今日头条", "百家号");
            }
            log.info("[生成标题] 生成平台: {}", platforms);

            List<Map<String, String>> allRows = Collections.synchronizedList(new ArrayList<>());
            Map<String, String> trackNameToIdMap = new HashMap<>();
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
            log.info("[生成标题] 总批次数: {}", totalBatches);

            final int finalTotalBatches = totalBatches;

            if (totalBatches == 0) {
                task.put("status", "failed");
                task.put("message", "所选平台和赛道没有可生成的组合，请检查平台与赛道的对应关系");
                log.warn("[生成标题] 任务失败: 所选平台和赛道没有可生成的组合");
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
                        log.info("[生成标题] 任务已取消 taskId={}", taskId);
                        return;
                    }

                    int batchEnd = Math.min(batchStart + batchSize, platformTracks.size());
                    List<Track> batchTracks = platformTracks.subList(batchStart, batchEnd);

                    task.put("message", "正在生成 " + platform + " 平台标题（批次 " + (batchStart / batchSize + 1) + "/" + (int) Math.ceil(platformTracks.size() / 5.0) + "）...");
                    log.info("[生成标题] 平台={} 批次={}/{} 赛道数={}", platform, (batchStart / batchSize + 1), (int) Math.ceil(platformTracks.size() / 5.0), batchTracks.size());

                    StringBuilder prompt = new StringBuilder();
                    prompt.append("请为\"").append(platform).append("\"平台生成爆款标题。\n\n");
                    prompt.append("需要生成标题的赛道：\n");
                    boolean hasSocialTrack = false;
                    for (int i = 0; i < batchTracks.size(); i++) {
                        Track t = batchTracks.get(i);
                        prompt.append(i + 1).append(". ").append(t.getName());
                        if (t.getIntro() != null && !t.getIntro().isEmpty()) {
                            prompt.append("（").append(t.getIntro()).append("）");
                        }
                        prompt.append("\n");
                        trackNameToIdMap.put(t.getName(), t.getId());
                        if (isSocialTrack(t.getName())) {
                            hasSocialTrack = true;
                        }
                    }
                    prompt.append("\n每个赛道生成").append(countPerCombo).append("个标题。要求：\n");
                    prompt.append("1. 标题是爆款风格，吸引眼球，适合").append(platform).append("传播\n");
                    if (hasSocialTrack) {
                        prompt.append("   【重要】对于社会民生类赛道（如涉及社会、民生、热点、时政、新闻等），标题必须基于本年度（").append(java.time.Year.now().getValue()).append("年）真实发生的事件或话题，严禁虚构、编造不存在的事件或数据。标题中涉及的时间、地点、人物、数字等必须真实可靠。\n");
                    }
                    prompt.append("2. 每个标题的 track 字段必须是上面给定的赛道名称（纯名称，不要包含括号内的说明），严禁自创赛道名称\n");
                    prompt.append("3. 每个标题必须配一段SEO描述（30-50字），要求：\n");
                    prompt.append("   - 包含赛道核心关键词，便于搜索引擎收录\n");
                    prompt.append("   - 突出文章价值点和读者收益\n");
                    prompt.append("   - 语言自然流畅，符合").append(platform).append("的搜索推荐算法偏好\n");
                    prompt.append("   - 适当使用数字、疑问、对比等提升点击率的手法\n");
                    prompt.append("4. 所有生成的标题必须全局唯一，同一批次内不同赛道之间不得出现相同或高度相似的标题\n");
                    prompt.append("5. 标题和描述中禁止出现英文双引号 \"，如有引用需求请使用中文引号「」或『』代替\n");
                    prompt.append("6. 只输出纯JSON，不要markdown代码块，不要任何额外文字\n\n");
                    prompt.append("格式：{\"titles\":[{\"track\":\"赛道名称\",\"title\":\"标题文字\",\"description\":\"SEO描述\"},...]}");
                    if (instruction != null && !instruction.trim().isEmpty()) {
                        prompt.append("\n\n【额外要求】").append(instruction.trim()).append("（请在生成标题时严格遵循此要求）");
                    }

                    log.info("[生成标题] Prompt 长度={} 内容前200字={}", prompt.length(), prompt.substring(0, Math.min(200, prompt.length())));
                    JsonNode arr = callClaude(prompt.toString());
                    if (arr == null) {
                        log.warn("[生成标题] AI 返回空结果，跳过本次批次 platform={} batch={}", platform, batchStart / batchSize + 1);
                        completedBatches++;
                        int progress = finalTotalBatches > 0 ? (completedBatches * 100 / finalTotalBatches) : 0;
                        task.put("progress", progress);
                        continue;
                    }
                    int titleCount = 0;
                    if (arr != null && arr.isArray()) {
                        titleCount = arr.size();
                        // 按赛道分组计数，每个赛道最多取 countPerCombo 条
                        Map<String, Integer> trackCountMap = new HashMap<>();
                        for (JsonNode node : arr) {
                            Map<String, String> row = new HashMap<>();
                            row.put("title", node.path("title").asText(""));
                            row.put("platform", platform);
                            // 清洗 track：去掉冒号/括号及后面的内容，防止 AI 把 intro 拼进来
                            String rawTrack = node.path("track").asText("");
                            String cleanTrack = rawTrack.split("[：:\\(（]", 2)[0].trim();
                            row.put("track", cleanTrack);
                            row.put("description", node.path("description").asText(""));
                            if (!row.get("title").isEmpty()) {
                                String track = row.get("track");
                                int current = trackCountMap.getOrDefault(track, 0);
                                if (current < countPerCombo) {
                                    allRows.add(row);
                                    trackCountMap.put(track, current + 1);
                                }
                            }
                        }
                    }
                    log.info("[生成标题] 平台={} 批次={}/{} 生成标题数={} 累计={}", platform, (batchStart / batchSize + 1), (int) Math.ceil(platformTracks.size() / 5.0), titleCount, allRows.size());

                    completedBatches++;
                    int progress = finalTotalBatches > 0 ? (completedBatches * 100 / finalTotalBatches) : 0;
                    task.put("progress", progress);
                }
            }

            task.put("message", "正在写入 Excel 文件...");
            log.info("[生成标题] 写入Excel 总行数={} 路径={}", allRows.size(), outputPath);
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
            int skipCount = 0;
            // 用于内存去重：同一批次内相同标题只保留一条
            Set<String> batchDedupSet = new HashSet<>();
            // 用于数据库去重：查询已存在的标题（按名称做幂等性）
            Set<String> existingSet = new HashSet<>();
            List<TitleLibrary> existingTitles = titleLibraryService.list();
            if (existingTitles != null) {
                for (TitleLibrary et : existingTitles) {
                    if (et.getTitle() != null) {
                        existingSet.add(et.getTitle());
                    }
                }
            }
            LocalDate tomorrow = LocalDate.now().plusDays(1);

            for (Map<String, String> row : allRows) {
                try {
                    String trackName = row.get("track");
                    String trackId = null;
                    if (trackName != null && !trackName.isEmpty()) {
                        trackId = trackNameToIdMap.get(trackName);
                        if (trackId == null) {
                            Track t = trackMapper.findByName(trackName);
                            if (t != null) {
                                trackId = t.getId();
                            }
                        }
                    }
                    String title = row.get("title");
                    String platform = row.get("platform");

                    // 内存去重：同一批次内相同标题
                    if (!batchDedupSet.add(title)) {
                        skipCount++;
                        continue;
                    }

                    // 数据库去重：已存在的标题（按名称幂等）跳过
                    if (existingSet.contains(title)) {
                        skipCount++;
                        continue;
                    }

                    TitleLibrary tl = new TitleLibrary();
                    tl.setTitle(title);
                    tl.setDescription(row.get("description"));
                    tl.setPlatform(platform);
                    tl.setTrackId(trackId);
                    tl.setUseCount(0);
                    tl.setPushDate(tomorrow);  // 设置推荐日期为明天，doCheck 才能查到
                    titleLibraryService.save(tl);
                    // 创建审核记录
                    try {
                        titleReviewService.createReviewRecord(tl.getId(), "ai_generated");
                    } catch (Exception e) {
                        log.error("[生成标题] 创建审核记录失败 title={}: {}", tl.getTitle(), e.getMessage());
                    }
                    savedCount++;
                } catch (Exception e) {
                    log.error("[生成标题] 单条入库失败 title={}: {}", row.get("title"), e.getMessage());
                }
            }

            task.put("status", "completed");
            task.put("progress", 100);
            task.put("total", allRows.size());
            task.put("message", "生成完成，共 " + allRows.size() + " 条标题，入库 " + savedCount + " 条，跳过重复 " + skipCount + " 条");
            task.put("path", finalPath);
            log.info("[生成标题] 任务完成 taskId={} 总生成={} 入库={} 跳过重复={} 路径={}", taskId, allRows.size(), savedCount, skipCount, finalPath);

        } catch (Exception e) {
            log.error("[生成标题] 任务异常 taskId={}: {}", taskId, e.getMessage(), e);
            task.put("status", "failed");
            task.put("message", "生成失败：" + e.getMessage());
        }
    }

    private JsonNode callClaude(String prompt) {
        Process process = null;
        try {
            List<String> command = new ArrayList<>();
            command.add("/opt/homebrew/bin/claude");
            command.add("-p");
            command.add(prompt);
            command.add("--output-format=json");
            command.add("--no-session-persistence");

            log.info("[callClaude] 执行命令: {} -p ... --output-format=json --no-session-persistence", command.get(0));
            log.info("[callClaude] Prompt长度={} 内容前200字={}", prompt.length(), prompt.substring(0, Math.min(200, prompt.length())));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(System.getProperty("user.home")));
            pb.redirectErrorStream(true);
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

            boolean finished = process.waitFor(300, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                log.error("[callClaude] 命令执行超时(300s)，强制终止进程");
                killClaudeProcess(process);
                readerThread.interrupt();
                return null;
            }

            readerThread.join(5000);
            int exitCode = process.exitValue();
            log.info("[callClaude] 命令退出码: {}", exitCode);

            String rawOutput = output.toString().trim();
            log.info("[callClaude] 原始输出长度={} 内容前500字={}", rawOutput.length(), rawOutput.substring(0, Math.min(500, rawOutput.length())));
            if (rawOutput.isEmpty()) {
                log.warn("[callClaude] 命令输出为空");
                return null;
            }

            String result = rawOutput;
            int jsonStart = rawOutput.indexOf('{');
            if (jsonStart > 0) {
                result = rawOutput.substring(jsonStart);
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(result);
            log.info("[callClaude] root结构: {}", root.toString());
            boolean isError = root.path("is_error").asBoolean(false);
            if (isError) {
                String errMsg = root.path("result").asText("");
                log.error("[callClaude] Claude CLI 返回错误: {}", errMsg);
                return null;
            }

            JsonNode resultNode = root.path("result");
            JsonNode titles = null;
            if (resultNode.isTextual()) {
                // result 是字符串，需要二次解析
                String innerJson = resultNode.asText("");
                log.info("[callClaude] result是字符串，内容前800字={}", innerJson.substring(0, Math.min(800, innerJson.length())));
                if (innerJson.isEmpty()) {
                    log.warn("[callClaude] result字段为空字符串");
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
                JsonNode innerRoot = null;
                try {
                    innerRoot = mapper.readTree(innerJson);
                } catch (Exception parseEx) {
                    log.warn("[callClaude] innerJson解析失败，尝试修复未转义引号: {}", parseEx.getMessage());
                    String fixed = fixUnescapedQuotesInJson(innerJson);
                    try {
                        innerRoot = mapper.readTree(fixed);
                        log.info("[callClaude] 修复后解析成功");
                    } catch (Exception fixEx) {
                        log.error("[callClaude] 修复后仍解析失败: {}", fixEx.getMessage());
                        return null;
                    }
                }
                titles = innerRoot.path("titles");
            } else if (resultNode.isObject() || resultNode.isArray()) {
                // result 直接是 JSON 对象/数组
                log.info("[callClaude] result是JSON节点，类型={}", resultNode.getNodeType());
                if (resultNode.isObject()) {
                    titles = resultNode.path("titles");
                } else if (resultNode.isArray()) {
                    titles = resultNode;
                }
            } else {
                log.warn("[callClaude] result字段未知类型: {}", resultNode.getNodeType());
                return null;
            }

            if (titles == null || titles.isMissingNode()) {
                log.warn("[callClaude] 未找到 titles 字段");
                return null;
            }
            log.info("[callClaude] 解析成功 titles数量={} 类型={}", titles.isArray() ? titles.size() : 0, titles.getNodeType());
            return titles;
        } catch (Exception e) {
            log.error("[callClaude] 调用异常: {}", e.getMessage(), e);
            killClaudeProcess(process);
            return null;
        }
    }

    /**
     * 强制杀死 claude 进程及其所有子进程。
     * Java 的 destroyForcibly 有时无法杀死处于睡眠/等待网络状态的进程，
     * 这里使用 kill -9 确保进程被彻底终止。
     */
    private void killClaudeProcess(Process process) {
        if (process == null) return;
        try {
            long pid = process.pid();
            // 先尝试标准方式
            process.destroyForcibly();
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            // 再用 kill -9 兜底
            try {
                Runtime.getRuntime().exec("kill -9 " + pid);
            } catch (Exception ignored) {}
            // 杀死所有子进程（防止 claude 启动的孙子进程残留）
            try {
                Runtime.getRuntime().exec("pkill -9 -P " + pid);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    private boolean isSocialTrack(String trackName) {
        if (trackName == null) return false;
        String lower = trackName.toLowerCase();
        return lower.contains("社会") || lower.contains("民生") || lower.contains("热点")
                || lower.contains("时政") || lower.contains("新闻") || lower.contains("时事")
                || lower.contains("财经") || lower.contains("政策");
    }

    /**
     * 修复 JSON 字符串值中未转义的双引号。
     * Claude 有时会在 title/description 中生成带双引号的内容但没有正确转义，
     * 导致 Jackson 解析失败。此方法将字符串值内部的未转义双引号替换为中文引号。
     */
    private String fixUnescapedQuotesInJson(String json) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                result.append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                result.append(c);
                escape = true;
                continue;
            }
            if (c == '"') {
                if (inString) {
                    // 检查这个引号后面是否跟的是 JSON 结构分隔符
                    int j = i + 1;
                    while (j < json.length() && Character.isWhitespace(json.charAt(j))) j++;
                    char next = j < json.length() ? json.charAt(j) : '\0';
                    // 如果后面是结构分隔符，则是合法的字符串结束符
                    if (next == ':' || next == ',' || next == '}' || next == ']' || next == '\0') {
                        result.append(c);
                        inString = false;
                    } else {
                        // 字符串值内部的未转义双引号，替换为中文右双引号
                        result.append('\u201d');
                    }
                } else {
                    result.append(c);
                    inString = true;
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
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
