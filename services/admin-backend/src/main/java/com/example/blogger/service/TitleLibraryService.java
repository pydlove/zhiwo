package com.example.blogger.service;

import com.example.blogger.entity.TitleLibrary;
import com.example.blogger.entity.TitleRecommendation;
import com.example.blogger.entity.Track;
import com.example.blogger.entity.UserTrack;
import com.example.blogger.mapper.EmailPushLogMapper;
import com.example.blogger.mapper.TitleLibraryMapper;
import com.example.blogger.mapper.TitleRecommendationMapper;
import com.example.blogger.mapper.TrackMapper;
import com.example.blogger.mapper.UserMapper;
import com.example.blogger.mapper.UserTrackMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public TitleLibraryService(TitleLibraryMapper titleLibraryMapper, TitleRecommendationMapper titleRecommendationMapper,
                               UserMapper userMapper, EmailPushLogMapper emailPushLogMapper,
                               UserTrackMapper userTrackMapper, TrackMapper trackMapper) {
        this.titleLibraryMapper = titleLibraryMapper;
        this.titleRecommendationMapper = titleRecommendationMapper;
        this.userMapper = userMapper;
        this.emailPushLogMapper = emailPushLogMapper;
        this.userTrackMapper = userTrackMapper;
        this.trackMapper = trackMapper;
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

    public List<TitleLibrary> search(String platform, String trackId, String keyword, String recommendUserName, String matched, String pushDate) {
        return titleLibraryMapper.search(platform, trackId, keyword, recommendUserName, matched, pushDate);
    }

    public Map<String, Object> searchPage(String platform, String trackId, String keyword, String recommendUserName, String matched, String pushDate, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<TitleLibrary> list = titleLibraryMapper.searchPage(platform, trackId, keyword, recommendUserName, matched, pushDate, offset, pageSize);
        int total = titleLibraryMapper.countSearch(platform, trackId, keyword, recommendUserName, matched, pushDate);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    public TitleLibrary getById(String id) {
        return titleLibraryMapper.findById(id);
    }

    public void save(TitleLibrary titleLibrary) {
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

    public List<Map<String, Object>> findUnrecommendedUsers(LocalDate date) {
        List<Map<String, Object>> allUsers = userMapper.findAllActiveUsers();
        List<Map<String, Object>> result = new ArrayList<>();
        List<String> unrecommendedUserIds = new ArrayList<>();
        for (Map<String, Object> user : allUsers) {
            String userId = (String) user.get("id");
            // 过滤掉非真实用户（is_real != 1）
            Object isRealObj = user.get("isReal");
            int isReal = 0;
            if (isRealObj instanceof Number) {
                isReal = ((Number) isRealObj).intValue();
            }
            if (isReal != 1) {
                continue;
            }
            int count = titleRecommendationMapper.countRecommendedByUserAndDate(userId, date);
            if (count == 0) {
                result.add(user);
                unrecommendedUserIds.add(userId);
            }
        }
        // 批量查询订阅信息
        if (!unrecommendedUserIds.isEmpty()) {
            List<Track> allTracks = trackMapper.findAll();
            Map<String, Track> trackMap = allTracks.stream().collect(Collectors.toMap(Track::getId, t -> t));
            List<UserTrack> userTracks = userTrackMapper.findByUserIds(unrecommendedUserIds);
            Map<String, List<String>> userTrackNames = new HashMap<>();
            for (UserTrack ut : userTracks) {
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

    public List<Map<String, Object>> findUnpushedUsers(LocalDate date) {
        List<Map<String, Object>> allUsers = userMapper.findAllActiveUsersWithEmail();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> user : allUsers) {
            String userId = (String) user.get("id");
            int count = emailPushLogMapper.countByUserAndDate(userId, date);
            if (count == 0) {
                result.add(user);
            }
        }
        return result;
    }
}
