package com.example.blogger.service;

import com.example.blogger.entity.SubscriptionPost;
import com.example.blogger.entity.TitleLibrary;
import com.example.blogger.entity.TitleRecommendation;
import com.example.blogger.entity.Track;
import com.example.blogger.entity.UserTrack;
import com.example.blogger.mapper.EmailPushLogMapper;
import com.example.blogger.mapper.SubscriptionPostMapper;
import com.example.blogger.mapper.TitleLibraryMapper;
import com.example.blogger.mapper.TitleRecommendationMapper;
import com.example.blogger.mapper.TrackMapper;
import com.example.blogger.mapper.UserMapper;
import com.example.blogger.mapper.UserTrackMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
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
    private final SubscriptionPostMapper subscriptionPostMapper;

    public TitleLibraryService(TitleLibraryMapper titleLibraryMapper, TitleRecommendationMapper titleRecommendationMapper,
                               UserMapper userMapper, EmailPushLogMapper emailPushLogMapper,
                               UserTrackMapper userTrackMapper, TrackMapper trackMapper,
                               SubscriptionPostMapper subscriptionPostMapper) {
        this.titleLibraryMapper = titleLibraryMapper;
        this.titleRecommendationMapper = titleRecommendationMapper;
        this.userMapper = userMapper;
        this.emailPushLogMapper = emailPushLogMapper;
        this.userTrackMapper = userTrackMapper;
        this.trackMapper = trackMapper;
        this.subscriptionPostMapper = subscriptionPostMapper;
    }

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

    public List<TitleLibrary> search(String platform, String trackId, String keyword, String recommendUserName, String matched, String pushDate, String isUsed) {
        return titleLibraryMapper.search(platform, trackId, keyword, recommendUserName, matched, pushDate, isUsed);
    }

    public Map<String, Object> searchPage(String platform, String trackId, String keyword, String recommendUserName, String matched, String pushDate, String isUsed, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<TitleLibrary> list = titleLibraryMapper.searchPage(platform, trackId, keyword, recommendUserName, matched, pushDate, isUsed, offset, pageSize);
        int total = titleLibraryMapper.countSearch(platform, trackId, keyword, recommendUserName, matched, pushDate, isUsed);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    public TitleLibrary getById(String id) {
        return titleLibraryMapper.findById(id);
    }

    public void save(TitleLibrary titleLibrary) {
        if (titleLibrary.getIsUsed() == null) {
            titleLibrary.setIsUsed(0);
        }
        if (titleLibrary.getId() == null || titleLibrary.getId().isEmpty()) {
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
                rec.setTrackId(titleLibrary.getTrackId());
                rec.setRecommendDate(titleLibrary.getRecommendDate() != null ? titleLibrary.getRecommendDate() : (titleLibrary.getPushDate() != null ? titleLibrary.getPushDate() : LocalDate.now()));
                titleRecommendationMapper.insert(rec);
            } else {
                // 显式传入空字符串时才清空关联
                titleRecommendationMapper.deleteByTitleId(titleId);
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

            // 过滤掉非真实用户（is_real != 1）
            if (toIntValue(user.get("isReal")) != 1) {
                continue;
            }

            // 按类型过滤
            if ("accountOpened".equals(type)) {
                if (toIntValue(user.get("isAccountOpened")) != 1) continue;
            } else if ("distributor".equals(type)) {
                if (toIntValue(user.get("isDistributor")) != 1) continue;
            } else if ("trial".equals(type)) {
                if (toIntValue(user.get("isTrial")) != 1) continue;
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

            Map<String, Object> user = userMap.computeIfAbsent(userId, k -> {
                Map<String, Object> u = new HashMap<>();
                u.put("userId", userId);
                u.put("username", row.get("username"));
                u.put("email", row.get("email"));
                u.put("isAccountOpened", row.get("isAccountOpened"));
                u.put("isDistributor", row.get("isDistributor"));
                u.put("isTrial", row.get("isTrial"));
                u.put("tracks", new ArrayList<Map<String, Object>>());
                u.put("totalTracks", 0);
                u.put("tracksWithPost", 0);
                u.put("isEmailPushed", false);
                return u;
            });

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tracks = (List<Map<String, Object>>) user.get("tracks");
            boolean hasPost = subscriptionPostId != null && !subscriptionPostId.isEmpty();

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
                tracks.add(track);
            }

            user.put("totalTracks", tracks.size());
            user.put("tracksWithPost", tracks.stream().filter(t -> Boolean.TRUE.equals(t.get("hasPost"))).count());
        }

        // Query email push status for each user
        for (Map<String, Object> user : userMap.values()) {
            String userId = (String) user.get("userId");
            int pushCount = emailPushLogMapper.countByUserAndDate(userId, date);
            user.put("isEmailPushed", pushCount > 0);
        }

        List<Map<String, Object>> users = new ArrayList<>(userMap.values());

        // Filter by type
        if (!"all".equals(type)) {
            String key = switch (type) {
                case "accountOpened" -> "isAccountOpened";
                case "distributor" -> "isDistributor";
                case "trial" -> "isTrial";
                default -> type;
            };
            users = users.stream().filter(u -> {
                Object val = u.get(key);
                return val instanceof Number ? ((Number) val).intValue() == 1 : Boolean.TRUE.equals(val);
            }).collect(Collectors.toList());
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
}
