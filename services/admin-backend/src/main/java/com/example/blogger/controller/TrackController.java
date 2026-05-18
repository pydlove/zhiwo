package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.Track;
import com.example.blogger.service.TrackService;
import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

@RestController
@RequestMapping("/api/tracks")
@CrossOrigin(origins = "*")
public class TrackController {
    private final TrackService trackService;
    private final DataSource dataSource;

    public TrackController(TrackService trackService, DataSource dataSource) {
        this.trackService = trackService;
        this.dataSource = dataSource;
    }

    @GetMapping
    public Result<List<Track>> list() {
        return Result.ok(trackService.list());
    }

    @GetMapping("/{id}")
    public Result<Track> get(@PathVariable String id) {
        return Result.ok(trackService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Track track) {
        trackService.save(track);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Track track) {
        track.setId(id);
        trackService.save(track);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        trackService.delete(id);
        return Result.ok(null);
    }

    /**
     * 手动触发合并重复赛道（不重启服务，直接调用）
     */
    @GetMapping("/force-merge-duplicates")
    public Result<Map<String, Object>> forceMergeDuplicateTracks() {
        trackService.forceMergeDuplicateTracks();
        Map<String, Object> result = new HashMap<>();
        result.put("message", "合并完成，请查看后端控制台日志");
        return Result.ok(result);
    }

    /**
     * 诊断接口：查询所有 name 重复的赛道
     */
    @GetMapping("/duplicate-check")
    public Result<Map<String, Object>> duplicateCheck() {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT name, COUNT(*) as cnt, GROUP_CONCAT(id ORDER BY created_at ASC SEPARATOR '|') as ids, " +
                "GROUP_CONCAT(created_at ORDER BY created_at ASC SEPARATOR '|') as dates " +
                "FROM tu_track WHERE is_deleted = 0 GROUP BY name HAVING cnt > 1"
            );
            List<Map<String, Object>> duplicates = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", rs.getString("name"));
                item.put("count", rs.getInt("cnt"));
                item.put("ids", rs.getString("ids"));
                item.put("dates", rs.getString("dates"));
                duplicates.add(item);
            }
            result.put("duplicates", duplicates);
            result.put("totalDuplicateGroups", duplicates.size());
            return Result.ok(result);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 合并指定名称的重复赛道：保留最早创建的，其余迁移后删除
     */
    @PostMapping("/merge-duplicates")
    public Result<Map<String, Object>> mergeDuplicates(@RequestBody Map<String, Object> body) {
        String trackName = (String) body.get("trackName");
        String keepId = (String) body.get("keepId"); // 可选：指定保留哪个ID
        if (trackName == null || trackName.isEmpty()) {
            return Result.error("请提供赛道名称 trackName");
        }
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // 1. 查询该名称下所有未删除的赛道
            ResultSet rs = stmt.executeQuery(
                "SELECT id, name, platforms, created_at FROM tu_track " +
                "WHERE is_deleted = 0 AND name = '" + trackName.replace("'", "\\'") + "' ORDER BY created_at ASC"
            );
            List<String> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getString("id"));
            }
            if (ids.size() < 2) {
                return Result.error("该赛道名称下只有 " + ids.size() + " 条记录，无需合并");
            }
            String targetId = keepId != null && ids.contains(keepId) ? keepId : ids.get(0);
            List<String> mergeIds = ids.stream().filter(id -> !id.equals(targetId)).toList();

            int updatedBloggers = 0, updatedTitles = 0, updatedUserTracks = 0;
            int updatedRecs = 0, updatedDaily = 0, updatedRef = 0;

            for (String oldId : mergeIds) {
                updatedBloggers += stmt.executeUpdate(
                    "UPDATE tu_blogger SET track_id = '" + targetId + "' WHERE track_id = '" + oldId + "'"
                );
                updatedTitles += stmt.executeUpdate(
                    "UPDATE tu_title_library SET track_id = '" + targetId + "' WHERE track_id = '" + oldId + "'"
                );
                updatedUserTracks += stmt.executeUpdate(
                    "UPDATE tu_user_track SET track_id = '" + targetId + "' WHERE track_id = '" + oldId + "'"
                );
                updatedRecs += stmt.executeUpdate(
                    "UPDATE tu_title_recommendation SET track_id = '" + targetId + "' WHERE track_id = '" + oldId + "'"
                );
                updatedDaily += stmt.executeUpdate(
                    "UPDATE tu_daily_recommend SET track_id = '" + targetId + "' WHERE track_id = '" + oldId + "'"
                );
                updatedRef += stmt.executeUpdate(
                    "UPDATE tu_reference_post SET track_id = '" + targetId + "' WHERE track_id = '" + oldId + "'"
                );
                // 逻辑删除旧赛道
                stmt.executeUpdate("UPDATE tu_track SET is_deleted = 1 WHERE id = '" + oldId + "'");
            }

            result.put("trackName", trackName);
            result.put("keepId", targetId);
            result.put("mergedIds", mergeIds);
            result.put("updatedBloggers", updatedBloggers);
            result.put("updatedTitles", updatedTitles);
            result.put("updatedUserTracks", updatedUserTracks);
            result.put("updatedRecommendations", updatedRecs);
            result.put("updatedDailyRecommends", updatedDaily);
            result.put("updatedReferencePosts", updatedRef);
            return Result.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("合并失败：" + e.getMessage());
        }
    }
}
