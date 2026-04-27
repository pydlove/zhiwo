package com.example.blogger.service;

import com.example.blogger.entity.TitleLibrary;
import com.example.blogger.entity.TitleRecommendation;
import com.example.blogger.mapper.TitleLibraryMapper;
import com.example.blogger.mapper.TitleRecommendationMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TitleLibraryService {

    private final TitleLibraryMapper titleLibraryMapper;
    private final TitleRecommendationMapper titleRecommendationMapper;

    public TitleLibraryService(TitleLibraryMapper titleLibraryMapper, TitleRecommendationMapper titleRecommendationMapper) {
        this.titleLibraryMapper = titleLibraryMapper;
        this.titleRecommendationMapper = titleRecommendationMapper;
    }

    public List<TitleLibrary> list() {
        return titleLibraryMapper.findAll(null, null);
    }

    public Map<String, Object> listPage(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<TitleLibrary> list = titleLibraryMapper.findAll(offset, pageSize);
        int total = titleLibraryMapper.countAll();
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    public List<TitleLibrary> search(String platform, String trackId, String keyword, String recommendUserName, String matched, String pushDate) {
        return titleLibraryMapper.search(platform, trackId, keyword, recommendUserName, matched, pushDate, null, null);
    }

    public Map<String, Object> searchPage(String platform, String trackId, String keyword, String recommendUserName, String matched, String pushDate, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<TitleLibrary> list = titleLibraryMapper.search(platform, trackId, keyword, recommendUserName, matched, pushDate, offset, pageSize);
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
}
