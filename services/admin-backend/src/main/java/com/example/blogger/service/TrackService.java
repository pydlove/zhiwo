package com.example.blogger.service;

import com.example.blogger.entity.Track;
import com.example.blogger.mapper.TrackMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TrackService {
    private final TrackMapper trackMapper;
    private final DataSource dataSource;

    public TrackService(TrackMapper trackMapper, DataSource dataSource) {
        this.trackMapper = trackMapper;
        this.dataSource = dataSource;
    }

    /* TODO: 临时逻辑 —— 启动时自动合并 name 重复的赛道，保留最早创建的，下次发布前请注释掉下面整个方法 */
    @PostConstruct
    public void initMergeDuplicateTracks() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT name, COUNT(*) as cnt, GROUP_CONCAT(id ORDER BY created_at ASC SEPARATOR '|') as ids " +
                "FROM tu_track WHERE is_deleted = 0 GROUP BY name HAVING cnt > 1"
            );
            int mergedGroups = 0;
            while (rs.next()) {
                String trackName = rs.getString("name");
                String[] ids = rs.getString("ids").split("\\|");
                if (ids.length < 2) continue;
                String keepId = ids[0];
                StringBuilder log = new StringBuilder("[合并重复赛道] name=" + trackName + ", 保留=" + keepId + ", 合并=");
                for (int i = 1; i < ids.length; i++) {
                    String oldId = ids[i];
                    log.append(oldId).append(" ");
                    stmt.executeUpdate("UPDATE tu_blogger SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    stmt.executeUpdate("UPDATE tu_title_library SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    stmt.executeUpdate("UPDATE tu_user_track SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    stmt.executeUpdate("UPDATE tu_title_recommendation SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    stmt.executeUpdate("UPDATE tu_daily_recommend SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    stmt.executeUpdate("UPDATE tu_reference_post SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    stmt.executeUpdate("UPDATE tu_subscription_post SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    stmt.executeUpdate("UPDATE tu_track SET is_deleted = 1 WHERE id = '" + oldId + "'");
                }
                System.out.println(log.toString().trim());
                mergedGroups++;
            }
            System.out.println("[合并重复赛道] 完成: 合并了 " + mergedGroups + " 组重复赛道");
        } catch (Exception e) {
            System.out.println("[合并重复赛道] 失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /* TODO: 临时逻辑结束 —— 下次请注释掉上面整个方法 */

    public List<Track> list() {
        return trackMapper.findAll();
    }

    public Track getById(String id) {
        return trackMapper.findById(id);
    }

    public void save(Track track) {
        if (track.getId() == null || track.getId().isEmpty()) {
            track.setId(UUID.randomUUID().toString().replace("-", ""));
            trackMapper.insert(track);
        } else {
            trackMapper.update(track);
        }
    }

    public void delete(String id) {
        trackMapper.delete(id);
    }
}
