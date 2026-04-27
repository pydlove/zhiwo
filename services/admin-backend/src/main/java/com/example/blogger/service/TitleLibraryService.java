package com.example.blogger.service;

import com.example.blogger.entity.TitleLibrary;
import com.example.blogger.entity.TitleRecommendation;
import com.example.blogger.mapper.TitleLibraryMapper;
import com.example.blogger.mapper.TitleRecommendationMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
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
        return titleLibraryMapper.findAll();
    }

    public List<TitleLibrary> search(String platform, String trackId, String keyword, String recommendUserName, String matched, String pushDate) {
        return titleLibraryMapper.search(platform, trackId, keyword, recommendUserName, matched, pushDate);
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
        if (recommendUserId != null && !recommendUserId.isEmpty()) {
            // 先清除旧关联，再插入新关联
            titleRecommendationMapper.deleteByTitleId(titleId);
            TitleRecommendation rec = new TitleRecommendation();
            rec.setId(UUID.randomUUID().toString().replace("-", ""));
            rec.setTitleLibraryId(titleId);
            rec.setUserId(recommendUserId);
            rec.setPlatform(titleLibrary.getPlatform());
            rec.setTrackId(titleLibrary.getTrackId());
            rec.setRecommendDate(titleLibrary.getPushDate() != null ? titleLibrary.getPushDate() : LocalDate.now());
            titleRecommendationMapper.insert(rec);
        } else if (titleLibrary.getId() != null && !titleLibrary.getId().isEmpty()) {
            // 编辑时清空用户关联
            titleRecommendationMapper.deleteByTitleId(titleId);
        }
    }

    public void delete(String id) {
        titleLibraryMapper.delete(id);
    }
}
