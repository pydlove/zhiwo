package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.TitleLibrary;
import com.example.blogger.entity.Track;
import com.example.blogger.mapper.TitleLibraryMapper;
import com.example.blogger.mapper.TrackMapper;
import com.example.blogger.service.TitleLibraryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 标题推送接收端接口
 * 用于接收其他环境推送过来的标题数据
 */
@RestController
@RequestMapping("/api/title-library")
@CrossOrigin(origins = "*")
public class TitlePushReceiveController {

    private static final Logger log = LoggerFactory.getLogger(TitlePushReceiveController.class);

    private final TitleLibraryMapper titleLibraryMapper;
    private final TitleLibraryService titleLibraryService;
    private final TrackMapper trackMapper;

    public TitlePushReceiveController(TitleLibraryMapper titleLibraryMapper, TitleLibraryService titleLibraryService, TrackMapper trackMapper) {
        this.titleLibraryMapper = titleLibraryMapper;
        this.titleLibraryService = titleLibraryService;
        this.trackMapper = trackMapper;
    }

    /**
     * 接收推送的标题数据
     * 幂等性：按 title + platform + track_id 判断，已存在则更新 description 和 use_count
     */
    @PostMapping("/push-receive")
    public Result<Map<String, Object>> receivePush(@RequestBody Map<String, Object> payload) {
        log.info("[push-receive] 收到推送请求: {}", payload);
        try {
            String title = (String) payload.get("title");
            String platform = (String) payload.get("platform");
            String trackId = (String) payload.get("trackId");

            if (title == null || title.isEmpty()) {
                log.warn("[push-receive] 标题为空，拒绝处理");
                return Result.error("标题不能为空");
            }

            String trackName = (String) payload.get("trackName");
            log.info("[push-receive] 原始trackId={}, trackName={}", trackId, trackName);

            // 如果推送带了 trackId，确保本地赛道记录存在且名称正确
            if (trackId != null && !trackId.isEmpty()) {
                Track localTrack = trackMapper.findById(trackId);
                log.info("[push-receive] 按id查找赛道: id={}, result={}", trackId, localTrack != null ? localTrack.getName() : "null");

                if (localTrack == null && trackName != null && !trackName.isEmpty()) {
                    // 按id找不到，尝试按名称查找（可能不同环境赛道id不同）
                    localTrack = trackMapper.findByName(trackName);
                    log.info("[push-receive] 按名称查找赛道: name={}, result={}", trackName, localTrack != null ? localTrack.getId() : "null");
                    if (localTrack != null) {
                        // 用本地trackId替换推送过来的trackId
                        trackId = localTrack.getId();
                        log.info("[push-receive] 复用本地赛道id: {} -> {}", payload.get("trackId"), trackId);
                    }
                }

                if (localTrack == null) {
                    // 本地完全没有这个赛道，创建新记录
                    Track newTrack = new Track();
                    newTrack.setId(trackId);
                    newTrack.setName(trackName != null && !trackName.isEmpty() ? trackName : "未命名赛道");
                    newTrack.setPlatforms(platform);
                    newTrack.setSortOrder(0);
                    newTrack.setIsHot(0);
                    newTrack.setIsDeleted(0);
                    try {
                        trackMapper.insert(newTrack);
                        log.info("[push-receive] 自动创建赛道记录: id={}, name={}", trackId, newTrack.getName());
                    } catch (Exception ex) {
                        log.warn("[push-receive] 自动创建赛道记录失败（可能已存在）: id={}, msg={}", trackId, ex.getMessage());
                    }
                } else if (trackName != null && !trackName.isEmpty()
                        && (localTrack.getName() == null || localTrack.getName().isEmpty() || "未命名赛道".equals(localTrack.getName()))) {
                    // 赛道已存在但名称为空或"未命名赛道"，更新为正确的名称
                    localTrack.setName(trackName);
                    localTrack.setPlatforms(platform);
                    try {
                        trackMapper.update(localTrack);
                        log.info("[push-receive] 更新赛道名称: id={}, name={}", localTrack.getId(), trackName);
                    } catch (Exception ex) {
                        log.warn("[push-receive] 更新赛道名称失败: id={}, msg={}", localTrack.getId(), ex.getMessage());
                    }
                }
            }

            // 按 title + platform + track_id 查找是否已存在
            TitleLibrary existing = titleLibraryMapper.findByTitlePlatformTrack(title, platform, trackId);
            log.info("[push-receive] 查找已存在记录: title={}, platform={}, trackId={}, existing={}",
                    title, platform, trackId, existing != null ? existing.getId() : "null");

            if (existing != null) {
                // 已存在，更新 description、trackId 和 use_count
                existing.setDescription((String) payload.get("description"));
                if (trackId != null) {
                    existing.setTrackId(trackId);
                }
                existing.setUseCount(existing.getUseCount() != null ? existing.getUseCount() + 1 : 1);
                titleLibraryService.save(existing);
                log.info("[push-receive] 更新已存在记录: id={}, oldTrackId={}, newTrackId={}", existing.getId(), existing.getTrackId(), trackId);
                return Result.ok(Map.of("id", existing.getId(), "action", "updated"));
            }

            // 不存在，插入新记录（不设置 id，让 service 自动生成并走 insert 分支）
            TitleLibrary newTitle = new TitleLibrary();
            newTitle.setTitle(title);
            newTitle.setDescription((String) payload.get("description"));
            newTitle.setPlatform(platform);
            newTitle.setTrackId(trackId);
            Object pushDate = payload.get("pushDate");
            if (pushDate != null && !pushDate.toString().isEmpty()) {
                try {
                    newTitle.setPushDate(java.time.LocalDate.parse(pushDate.toString()));
                } catch (Exception ignored) {}
            }
            Object useCount = payload.get("useCount");
            newTitle.setUseCount(useCount != null ? Integer.parseInt(useCount.toString()) : 0);
            Object isUsed = payload.get("isUsed");
            newTitle.setIsUsed(isUsed != null ? Integer.parseInt(isUsed.toString()) : 0);
            titleLibraryService.save(newTitle);
            log.info("[push-receive] 插入新记录: id={}, title={}, trackId={}", newTitle.getId(), title, trackId);

            return Result.ok(Map.of("id", newTitle.getId(), "action", "created"));

        } catch (Exception e) {
            log.error("[push-receive] 处理推送请求失败", e);
            return Result.error("接收推送失败：" + e.getMessage());
        }
    }
}
