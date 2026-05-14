package com.example.blogger.service;

import com.example.blogger.entity.EmailPushLog;
import com.example.blogger.entity.SubscriptionPost;
import com.example.blogger.entity.TitleLibrary;
import com.example.blogger.entity.TitleRecommendation;
import com.example.blogger.entity.Track;
import com.example.blogger.entity.User;
import com.example.blogger.entity.UserTrack;
import com.example.blogger.mapper.EmailPushLogMapper;
import com.example.blogger.mapper.SubscriptionPostMapper;
import com.example.blogger.mapper.TitleLibraryMapper;
import com.example.blogger.mapper.TitleRecommendationMapper;
import com.example.blogger.mapper.TrackMapper;
import com.example.blogger.mapper.UserMapper;
import com.example.blogger.mapper.UserTrackMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TitleLibraryService {

    private final TitleLibraryMapper titleLibraryMapper;
    private final TitleRecommendationMapper titleRecommendationMapper;
    private final UserMapper userMapper;
    private final EmailPushLogMapper emailPushLogMapper;
    private final UserTrackMapper userTrackMapper;
    private final TrackMapper trackMapper;
    private final EmailService emailService;
    private final SubscriptionPostMapper subscriptionPostMapper;

    public TitleLibraryService(TitleLibraryMapper titleLibraryMapper, TitleRecommendationMapper titleRecommendationMapper,
                               UserMapper userMapper, EmailPushLogMapper emailPushLogMapper,
                               UserTrackMapper userTrackMapper, TrackMapper trackMapper,
                               SubscriptionPostMapper subscriptionPostMapper, EmailService emailService) {
        this.titleLibraryMapper = titleLibraryMapper;
        this.titleRecommendationMapper = titleRecommendationMapper;
        this.userMapper = userMapper;
        this.emailPushLogMapper = emailPushLogMapper;
        this.userTrackMapper = userTrackMapper;
        this.trackMapper = trackMapper;
        this.subscriptionPostMapper = subscriptionPostMapper;
        this.emailService = emailService;
    }

    /**
     * 启动时自动填充标题库中 platform 为空的记录
     * 根据 track_id 查找赛道，取赛道的第一个 platform 填充
     */
    @PostConstruct
    public void initFillEmptyPlatform() {
        List<TitleLibrary> emptyList = titleLibraryMapper.findByEmptyPlatform();
        int filled = 0;
        int skipped = 0;
        for (TitleLibrary tl : emptyList) {
            String trackId = tl.getTrackId();
            if (trackId == null || trackId.isEmpty()) {
                skipped++;
                System.out.println("[启动填充platform] 跳过(无trackId): " + tl.getTitle());
                continue;
            }
            Track track = trackMapper.findById(trackId);
            if (track == null || track.getPlatforms() == null || track.getPlatforms().isEmpty()) {
                skipped++;
                System.out.println("[启动填充platform] 跳过(赛道无platform): " + tl.getTitle() + " trackId=" + trackId);
                continue;
            }
            String platform = track.getPlatforms();
            if (platform == null || platform.isEmpty()) {
                skipped++;
                continue;
            }
            titleLibraryMapper.updatePlatform(tl.getId(), platform);
            filled++;
            System.out.println("[启动填充platform] 填充: " + tl.getTitle() + " -> " + platform);
        }
        System.out.println("[启动填充platform] 完成: 填充=" + filled + ", 跳过=" + skipped);
    }

    /* TODO: 临时逻辑 —— 启动时删除 track_id 指向已不存在赛道的脏数据标题，下次发布前请注释掉下面整个方法 */
    @PostConstruct
    public void initDeleteOrphanTitles() {
        int deleted = titleLibraryMapper.deleteOrphanTitles();
        System.out.println("[临时] 启动自动删除孤儿标题(track_id指向已删除赛道): " + deleted + " 条");
    }
    /* TODO: 临时逻辑结束 —— 下次请注释掉上面整个方法 */

    public List<TitleLibrary> list() {
        return titleLibraryMapper.findAll();
    }

    public Map<String, Object> listPage(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<TitleLibrary> list = titleLibraryMapper.findAllPage(offset, pageSize);
        int total = titleLibraryMapper.countAll();
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    public List<TitleLibrary> search(String platform, String trackId, String keyword, String recommendUserName, String matched, String pushDate, String isUsed, String isConfirmed, String aiFlavor, String userType, String matchable, String sortField, String sortOrder) {
        return titleLibraryMapper.search(platform, trackId, keyword, recommendUserName, matched, pushDate, isUsed, isConfirmed, aiFlavor, userType, matchable, sortField, sortOrder);
    }

    public Map<String, Object> searchPage(String platform, String trackId, String keyword, String recommendUserName, String matched, String pushDate, String isUsed, String isConfirmed, String aiFlavor, String userType, String matchable, int page, int pageSize, String sortField, String sortOrder) {
        int offset = (page - 1) * pageSize;
        List<TitleLibrary> list = titleLibraryMapper.searchPage(platform, trackId, keyword, recommendUserName, matched, pushDate, isUsed, isConfirmed, aiFlavor, userType, matchable, offset, pageSize, sortField, sortOrder);
        int total = titleLibraryMapper.countSearch(platform, trackId, keyword, recommendUserName, matched, pushDate, isUsed, isConfirmed, aiFlavor, userType, matchable);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    public TitleLibrary getById(String id) {
        return titleLibraryMapper.findById(id);
    }

    public List<TitleLibrary> getAllUnmatchedTitles() {
        return titleLibraryMapper.findAllUnmatched();
    }

    public void save(TitleLibrary titleLibrary) {
        // 标题唯一性校验
        if (titleLibrary.getTitle() != null && !titleLibrary.getTitle().trim().isEmpty()) {
            TitleLibrary existing = titleLibraryMapper.findByTitle(titleLibrary.getTitle().trim());
            if (existing != null && !existing.getId().equals(titleLibrary.getId())) {
                throw new IllegalArgumentException("标题名称已存在：" + titleLibrary.getTitle().trim());
            }
        }

        if (titleLibrary.getId() == null || titleLibrary.getId().isEmpty()) {
            if (titleLibrary.getIsUsed() == null) {
                titleLibrary.setIsUsed(0);
            }
            titleLibrary.setId(UUID.randomUUID().toString().replace("-", ""));
            titleLibrary.setUseCount(0);
            titleLibraryMapper.insert(titleLibrary);
        } else {
            titleLibraryMapper.update(titleLibrary);
        }

        // 处理用户关联
        String titleId = titleLibrary.getId();
        String recommendUserId = titleLibrary.getRecommendUserId();
        if (recommendUserId != null) {
            if (!recommendUserId.isEmpty()) {
                // 有推荐用户：更新关联
                titleRecommendationMapper.deleteByTitleId(titleId);
                TitleRecommendation rec = new TitleRecommendation();
                rec.setId(UUID.randomUUID().toString().replace("-", ""));
                rec.setTitleLibraryId(titleId);
                rec.setUserId(recommendUserId);
                rec.setPlatform(titleLibrary.getPlatform());
                // track_id 使用用户订阅的赛道：优先匹配标题所属赛道，否则取用户订阅的第一个
                String titleTrackId = titleLibrary.getTrackId();
                List<UserTrack> userTracks = userTrackMapper.findByUserId(recommendUserId);
                String recTrackId = titleTrackId;
                if (userTracks != null && !userTracks.isEmpty()) {
                    recTrackId = userTracks.stream()
                            .map(UserTrack::getTrackId)
                            .filter(tid -> tid != null && tid.equals(titleTrackId))
                            .findFirst()
                            .orElse(userTracks.get(0).getTrackId());
                }
                rec.setTrackId(recTrackId);
                // 优先使用 pushDate（前端编辑时用户修改的是 pushDate），其次才回退到 recommendDate
                rec.setRecommendDate(titleLibrary.getPushDate() != null ? titleLibrary.getPushDate() : (titleLibrary.getRecommendDate() != null ? titleLibrary.getRecommendDate() : LocalDate.now()));
                titleRecommendationMapper.insert(rec);
                titleLibrary.setIsUsed(1);
                titleLibraryMapper.update(titleLibrary);
            } else {
                // 显式传入空字符串时才清空关联
                titleRecommendationMapper.deleteByTitleId(titleId);
                titleLibrary.setIsUsed(0);
                titleLibraryMapper.update(titleLibrary);
            }
        }
        // recommendUserId 为 null 时不处理关联，保持原样（防止导入等批量操作误删关联）
    }

    public void delete(String id) {
        titleLibraryMapper.delete(id);
    }

    public void updateIsUsed(String id, Integer isUsed) {
        titleLibraryMapper.updateIsUsed(id, isUsed);
    }

    public void updateAiFlavorStatus(String id, Integer aiFlavorStatus) {
        titleLibraryMapper.updateAiFlavorStatus(id, aiFlavorStatus);
    }

    public void updateIsCopied(String id, Integer isCopied) {
        titleLibraryMapper.updateIsCopied(id, isCopied);
    }

    public void updateIsConfirmed(String id, Integer isConfirmed) {
        titleLibraryMapper.updateIsConfirmed(id, isConfirmed);
    }

    public void updateConfirmStatus(String id, Integer confirmStatus) {
        titleLibraryMapper.updateConfirmStatus(id, confirmStatus);
    }

    public List<TitleLibrary> findPendingReview(String recommendDate) {
        return titleLibraryMapper.findPendingReview(recommendDate);
    }

    public List<TitleLibrary> findReviewHistory(String recommendDate) {
        return titleLibraryMapper.findReviewHistory(recommendDate);
    }

    public void updateGeneratedFile(String id, String fileUrl, String fileName) {
        titleLibraryMapper.updateGeneratedFile(id, fileUrl, fileName, LocalDateTime.now());
    }

    public void updateGenerateStatus(String id, Integer generateStatus) {
        titleLibraryMapper.updateGenerateStatus(id, generateStatus);
    }

    public void updateTitle(TitleLibrary title) {
        titleLibraryMapper.update(title);
    }

    public int batchMarkUsedForMatched() {
        return titleLibraryMapper.batchMarkUsedForMatched();
    }

    private int toIntValue(Object val) {
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        if (val instanceof Boolean) {
            return ((Boolean) val) ? 1 : 0;
        }
        return 0;
    }

    public List<Map<String, Object>> findUnrecommendedUsers(LocalDate date, String type) {
        List<Map<String, Object>> allUsers = userMapper.findAllActiveUsers();
        List<Map<String, Object>> result = new ArrayList<>();
        List<String> unrecommendedUserIds = new ArrayList<>();

        for (Map<String, Object> user : allUsers) {
            String userId = (String) user.get("id");

            // 按类型过滤
            Integer userTypeVal = user.get("userType") != null ? ((Number) user.get("userType")).intValue() : null;
            if ("accountOpened".equals(type)) {
                if (userTypeVal == null || userTypeVal != 1) continue;
            } else if ("distributor".equals(type)) {
                if (userTypeVal == null || userTypeVal != 2) continue;
            } else if ("trial".equals(type)) {
                if (userTypeVal == null || userTypeVal != 3) continue;
            }

            // 获取用户订阅的赛道
            List<UserTrack> userTracks = userTrackMapper.findByUserId(userId);
            if (userTracks == null || userTracks.isEmpty()) {
                continue; // 没有订阅赛道，不算未推荐
            }

            // 获取用户当日已有有效推荐（有关联文章）的赛道
            List<String> recommendedTrackIds = titleRecommendationMapper.findRecommendedTrackIdsByUserAndDate(userId, date);
            Set<String> recommendedSet = new HashSet<>(recommendedTrackIds != null ? recommendedTrackIds : new ArrayList<>());

            // 检查是否所有订阅赛道都有有效推荐
            boolean allRecommended = true;
            List<String> missingTrackNames = new ArrayList<>();
            for (UserTrack ut : userTracks) {
                if (!recommendedSet.contains(ut.getTrackId())) {
                    allRecommended = false;
                    Track track = trackMapper.findById(ut.getTrackId());
                    if (track != null) {
                        missingTrackNames.add(track.getPlatforms() + "-" + track.getName());
                    }
                }
            }

            if (!allRecommended) {
                user.put("missingTracks", String.join(", ", missingTrackNames));
                result.add(user);
                unrecommendedUserIds.add(userId);
            }
        }

        // 批量查询订阅信息
        if (!unrecommendedUserIds.isEmpty()) {
            List<Track> allTracks = trackMapper.findAll();
            Map<String, Track> trackMap = allTracks.stream().collect(Collectors.toMap(Track::getId, t -> t));
            List<UserTrack> allUserTracks = userTrackMapper.findByUserIds(unrecommendedUserIds);
            Map<String, List<String>> userTrackNames = new HashMap<>();
            for (UserTrack ut : allUserTracks) {
                Track track = trackMap.get(ut.getTrackId());
                if (track != null) {
                    String label = track.getPlatforms() + "-" + track.getName();
                    userTrackNames.computeIfAbsent(ut.getUserId(), k -> new ArrayList<>()).add(label);
                }
            }
            for (Map<String, Object> user : result) {
                String userId = (String) user.get("id");
                List<String> subs = userTrackNames.getOrDefault(userId, new ArrayList<>());
                user.put("trackInfo", String.join(", ", subs));
            }
        }
        return result;
    }

    public Map<String, Object> findPushOverview(LocalDate date, String type, String keyword,
                                                 String emailPushed, String articleComplete,
                                                 int page, int pageSize) {
        List<Map<String, Object>> rows = titleLibraryMapper.findPushOverview(date);
        Map<String, Map<String, Object>> userMap = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            String userId = (String) row.get("userId");
            String trackId = (String) row.get("trackId");
            String trackName = (String) row.get("trackName");
            String recommendationId = (String) row.get("recommendationId");
            String subscriptionPostId = row.get("subscriptionPostId") != null ? row.get("subscriptionPostId").toString() : null;
            String postTitle = (String) row.get("postTitle");
            String titleLibraryId = row.get("titleLibraryId") != null ? row.get("titleLibraryId").toString() : null;
            String titleName = (String) row.get("titleName");
            String generatedFileUrl = row.get("generatedFileUrl") != null ? row.get("generatedFileUrl").toString() : null;

            Map<String, Object> user = userMap.computeIfAbsent(userId, k -> {
                Map<String, Object> u = new HashMap<>();
                u.put("userId", userId);
                u.put("username", row.get("username"));
                u.put("wxName", row.get("wxName"));
                u.put("email", row.get("email"));
                u.put("userType", row.get("userType"));
                u.put("tracks", new ArrayList<Map<String, Object>>());
                u.put("totalTracks", 0);
                u.put("tracksWithPost", 0);
                u.put("tracksWithTitle", 0);
                u.put("isEmailPushed", false);
                return u;
            });

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tracks = (List<Map<String, Object>>) user.get("tracks");
            boolean hasPost = (subscriptionPostId != null && !subscriptionPostId.isEmpty())
                    || (generatedFileUrl != null && !generatedFileUrl.isEmpty());
            boolean hasTitle = titleLibraryId != null && !titleLibraryId.isEmpty();

            // 防御重复：同一赛道可能因推荐表多条记录导致重复行，去重并保留有文章的那条
            boolean found = false;
            for (Map<String, Object> existing : tracks) {
                if (trackId.equals(existing.get("trackId"))) {
                    found = true;
                    if (hasPost && !Boolean.TRUE.equals(existing.get("hasPost"))) {
                        existing.put("hasPost", true);
                        existing.put("recommendationId", recommendationId);
                        existing.put("subscriptionPostId", subscriptionPostId);
                        existing.put("postTitle", postTitle);
                    }
                    if (hasTitle && existing.get("titleName") == null) {
                        existing.put("titleLibraryId", titleLibraryId);
                        existing.put("titleName", titleName);
                    }
                    break;
                }
            }
            if (!found) {
                Map<String, Object> track = new HashMap<>();
                track.put("trackId", trackId);
                track.put("trackName", trackName);
                track.put("hasPost", hasPost);
                track.put("recommendationId", recommendationId);
                track.put("subscriptionPostId", subscriptionPostId);
                track.put("postTitle", postTitle);
                track.put("titleLibraryId", titleLibraryId);
                track.put("titleName", titleName);
                tracks.add(track);
            }

            user.put("totalTracks", tracks.size());
            user.put("tracksWithPost", tracks.stream().filter(t -> Boolean.TRUE.equals(t.get("hasPost"))).count());
            user.put("tracksWithTitle", tracks.stream().filter(t -> t.get("titleName") != null).count());
        }

        // Query email push status for each user (per title)
        for (Map<String, Object> user : userMap.values()) {
            String userId = (String) user.get("userId");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tracks = (List<Map<String, Object>>) user.get("tracks");
            List<String> pushedTitleIds = emailPushLogMapper.findPushedTitleIdsByUserAndDate(userId, date);
            Set<String> pushedSet = new HashSet<>(pushedTitleIds != null ? pushedTitleIds : new ArrayList<>());

            int tracksPushed = 0;
            int tracksUnpushed = 0;
            for (Map<String, Object> track : tracks) {
                String tlibId = track.get("titleLibraryId") != null ? track.get("titleLibraryId").toString() : null;
                boolean hasPost = Boolean.TRUE.equals(track.get("hasPost"));
                boolean isPushed = tlibId != null && !tlibId.isEmpty() && pushedSet.contains(tlibId);
                track.put("isPushed", isPushed);
                if (hasPost) {
                    if (isPushed) {
                        tracksPushed++;
                    } else {
                        tracksUnpushed++;
                    }
                }
            }
            user.put("tracksPushed", tracksPushed);
            user.put("tracksUnpushed", tracksUnpushed);
            user.put("isEmailPushed", tracksUnpushed == 0 && tracksPushed > 0);
        }

        List<Map<String, Object>> users = new ArrayList<>(userMap.values());

        // Filter by type
        if (type != null && !type.isEmpty() && !"all".equals(type)) {
            Integer targetType = switch (type) {
                case "accountOpened" -> 1;
                case "distributor" -> 2;
                case "trial" -> 3;
                default -> null;
            };
            if (targetType != null) {
                users = users.stream().filter(u -> targetType.equals(u.get("userType"))).collect(Collectors.toList());
            }
        }

        // Filter by keyword
        if (keyword != null && !keyword.isEmpty()) {
            String kw = keyword.toLowerCase();
            users = users.stream().filter(u -> {
                String username = (String) u.get("username");
                String email = (String) u.get("email");
                return (username != null && username.toLowerCase().contains(kw))
                    || (email != null && email.toLowerCase().contains(kw));
            }).collect(Collectors.toList());
        }

        // Filter by email pushed status
        if (emailPushed != null && !emailPushed.isEmpty()) {
            boolean pushed = "1".equals(emailPushed);
            users = users.stream().filter(u -> pushed == Boolean.TRUE.equals(u.get("isEmailPushed")))
                .collect(Collectors.toList());
        }

        // Filter by article complete status
        if (articleComplete != null && !articleComplete.isEmpty()) {
            boolean complete = "1".equals(articleComplete);
            users = users.stream().filter(u -> {
                int total = ((Number) u.getOrDefault("totalTracks", 0)).intValue();
                int withPost = ((Number) u.getOrDefault("tracksWithPost", 0)).intValue();
                boolean isComplete = total > 0 && withPost == total;
                return complete == isComplete;
            }).collect(Collectors.toList());
        }

        int total = users.size();
        int offset = (page - 1) * pageSize;
        List<Map<String, Object>> pageList = new ArrayList<>();
        for (int i = offset; i < Math.min(offset + pageSize, total); i++) {
            pageList.add(users.get(i));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageList);
        result.put("total", total);
        return result;
    }

    public List<Map<String, Object>> countByTrack() {
        return titleLibraryMapper.countByTrack();
    }

    public List<Map<String, Object>> findUnpushedUsers(LocalDate date) {
        List<Map<String, Object>> allUsers = userMapper.findAllActiveUsersWithEmail();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> user : allUsers) {
            String userId = (String) user.get("id");
            int count = emailPushLogMapper.countByUserAndDate(userId, date);
            if (count > 0) {
                continue;
            }
            // 查询该用户当日有关联文章的推荐
            List<Map<String, Object>> recommendations = titleRecommendationMapper.findByUserAndDate(userId, date);
            List<String> postTitles = new ArrayList<>();
            if (recommendations != null) {
                for (Map<String, Object> rec : recommendations) {
                    Object postIdObj = rec.get("subscription_post_id");
                    if (postIdObj == null || postIdObj.toString().isEmpty()) {
                        continue;
                    }
                    SubscriptionPost post = subscriptionPostMapper.findById(postIdObj.toString());
                    if (post != null && post.getTitle() != null && !post.getTitle().isEmpty()) {
                        postTitles.add(post.getTitle());
                    }
                }
            }
            // 只返回有关联文章的用户
            if (!postTitles.isEmpty()) {
                user.put("postTitles", postTitles);
                result.add(user);
            }
        }
return result;
    }

    public void batchPushEmailForScheduled(String dateStr, List<String> userIds) {
        LocalDate pushDate = LocalDate.parse(dateStr);
        for (String userId : userIds) {
            try {
                User user = userMapper.findById(userId);
                if (user == null) continue;
                if (user.getStatus() == null || user.getStatus() != 1) continue;
                if (user.getEmail() == null || user.getEmail().isEmpty()) continue;
                List<Map<String, Object>> recMaps = titleRecommendationMapper.findByUserAndDate(userId, pushDate);
                if (recMaps == null || recMaps.isEmpty()) continue;
                List<String> pushedTitleIds = emailPushLogMapper.findPushedTitleIdsByUserAndDate(userId, pushDate);
                Set<String> pushedSet = new HashSet<>(pushedTitleIds != null ? pushedTitleIds : new ArrayList<>());
                boolean anyPushed = false;
                for (Map<String, Object> recMap : recMaps) {
                    String subPostId = recMap.get("subscription_post_id") != null ? recMap.get("subscription_post_id").toString() : null;
                    String trackId = recMap.get("track_id") != null ? recMap.get("track_id").toString() : null;
                    String titleLibId = recMap.get("title_library_id") != null ? recMap.get("title_library_id").toString() : null;
                    if (titleLibId != null && !titleLibId.isEmpty() && pushedSet.contains(titleLibId)) continue;
                    if (subPostId == null || subPostId.isEmpty()) continue;
                    SubscriptionPost post = subscriptionPostMapper.findById(subPostId);
                    if (post == null || post.getFileUrl() == null || post.getFileUrl().isEmpty()) continue;
                    File articleFile = new File(post.getFileUrl().startsWith("/")
                            ? System.getProperty("user.dir") + post.getFileUrl()
                            : post.getFileUrl());
                    if (!articleFile.exists()) {
                        articleFile = new File(System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "articles" + File.separator + post.getFileName());
                    }
                    if (!articleFile.exists()) continue;
                    Track userTrack = trackMapper.findById(trackId);
                    String trackName = userTrack != null ? userTrack.getName() : "";
                    TitleLibrary titleLib = this.getById(titleLibId);
                    String articleTitle = titleLib != null ? titleLib.getTitle() : post.getTitle();
                    String platform = titleLib != null && titleLib.getPlatform() != null ? titleLib.getPlatform() : "";
                    emailService.sendDailyRecommendEmail(user.getEmail(), user.getUsername(), trackName, articleTitle, platform, articleFile, post.getFileName());
                    EmailPushLog log = new EmailPushLog();
                    log.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
                    log.setUserId(userId);
                    log.setPushDate(pushDate);
                    log.setType("daily_recommend");
                    log.setTitleLibraryId(titleLibId);
                    emailPushLogMapper.insert(log);
                    anyPushed = true;
                }
            } catch (Exception e) {
                // 单用户失败不影响其他用户
            }
        }
    }
}
