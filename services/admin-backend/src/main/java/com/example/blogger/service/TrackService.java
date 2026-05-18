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

    /* TODO: 临时逻辑已注释 —— 启动时自动合并 name 重复的赛道
    @PostConstruct
    public void initMergeDuplicateTracks() {
        forceMergeDuplicateTracks();
    }
    */

    public void forceMergeDuplicateTracks() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            // ===== 第1步：强制合并已知的两个重复赛道ID =====
            // 用户报告: 473c87bf881d4136a8abb02f2e699804 和 fa6bda74d24543a0b260eea43b1c33c2 是重复的"社会民生"
            String knownId1 = "473c87bf881d4136a8abb02f2e699804";
            String knownId2 = "fa6bda74d24543a0b260eea43b1c33c2";
            ResultSet knownRs = stmt.executeQuery(
                "SELECT id, name, created_at FROM tu_track WHERE is_deleted = 0 AND id IN ('" + knownId1 + "','" + knownId2 + "') ORDER BY created_at ASC"
            );
            List<String> knownIds = new ArrayList<>();
            while (knownRs.next()) {
                knownIds.add(knownRs.getString("id"));
                System.out.println("[合并重复赛道] 已知重复: id=" + knownRs.getString("id")
                    + ", name=[" + knownRs.getString("name") + "]"
                    + ", created=" + knownRs.getTimestamp("created_at"));
            }
            if (knownIds.size() >= 2) {
                String keepId = knownIds.get(0);
                for (int i = 1; i < knownIds.size(); i++) {
                    String oldId = knownIds.get(i);
                    System.out.println("[合并重复赛道] 开始合并 oldId=" + oldId + " -> keepId=" + keepId);
                    int u1 = stmt.executeUpdate("UPDATE tu_blogger SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    int u2 = stmt.executeUpdate("UPDATE tu_title_library SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    int u3 = stmt.executeUpdate("UPDATE tu_user_track SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    int u4 = stmt.executeUpdate("UPDATE tu_title_recommendation SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    int u5 = stmt.executeUpdate("UPDATE tu_daily_recommend SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    int u6 = stmt.executeUpdate("UPDATE tu_reference_post SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    // 关键：先查询当前 is_deleted 值
                    ResultSet beforeRs = stmt.executeQuery("SELECT id, name, is_deleted FROM tu_track WHERE id = '" + oldId + "'");
                    if (beforeRs.next()) {
                        System.out.println("[合并重复赛道] 更新前: id=" + beforeRs.getString("id")
                            + ", name=[" + beforeRs.getString("name") + "]"
                            + ", is_deleted=" + beforeRs.getInt("is_deleted"));
                    }
                    int u8 = stmt.executeUpdate("UPDATE tu_track SET is_deleted = 1 WHERE id = '" + oldId + "'");
                    // 更新后立即验证
                    ResultSet afterRs = stmt.executeQuery("SELECT id, name, is_deleted FROM tu_track WHERE id = '" + oldId + "'");
                    if (afterRs.next()) {
                        System.out.println("[合并重复赛道] 更新后: id=" + afterRs.getString("id")
                            + ", name=[" + afterRs.getString("name") + "]"
                            + ", is_deleted=" + afterRs.getInt("is_deleted")
                            + ", updateRows=" + u8);
                    }
                    System.out.println("[合并重复赛道] 强制合并已知: " + oldId + " -> " + keepId
                        + " (blogger=" + u1 + ", title=" + u2 + ", userTrack=" + u3 + ", rec=" + u4
                        + ", daily=" + u5 + ", ref=" + u6 + ", del=" + u8 + ")");
                }
            } else {
                System.out.println("[合并重复赛道] 已知ID查询结果不足2条，实际=" + knownIds.size());
            }

            // ===== 第2步：通用检测其他重复赛道 =====
            ResultSet allRs = stmt.executeQuery(
                "SELECT id, name, HEX(name) as nameHex, LENGTH(name) as nameLen, platforms, created_at FROM tu_track WHERE is_deleted = 0 ORDER BY name"
            );
            System.out.println("[合并重复赛道] 当前所有未删除赛道:");
            while (allRs.next()) {
                System.out.println("  id=" + allRs.getString("id")
                    + ", name=[" + allRs.getString("name") + "]"
                    + ", len=" + allRs.getInt("nameLen")
                    + ", hex=" + allRs.getString("nameHex")
                    + ", platforms=" + allRs.getString("platforms"));
            }

            ResultSet rs = stmt.executeQuery(
                "SELECT TRIM(name) as cleanName, COUNT(*) as cnt, " +
                "GROUP_CONCAT(id ORDER BY created_at ASC SEPARATOR '|') as ids " +
                "FROM tu_track WHERE is_deleted = 0 GROUP BY TRIM(name) HAVING cnt > 1"
            );
            int mergedGroups = 0;
            while (rs.next()) {
                String trackName = rs.getString("cleanName");
                String idsStr = rs.getString("ids");
                if (idsStr == null || idsStr.isEmpty()) continue;
                String[] ids = idsStr.split("\\|");
                if (ids.length < 2) continue;
                String keepId = ids[0];
                System.out.println("[合并重复赛道] 发现重复: name=[" + trackName + "], 共 " + ids.length + " 条, 保留=" + keepId);
                for (int i = 1; i < ids.length; i++) {
                    String oldId = ids[i];
                    int u1 = stmt.executeUpdate("UPDATE tu_blogger SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    int u2 = stmt.executeUpdate("UPDATE tu_title_library SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    int u3 = stmt.executeUpdate("UPDATE tu_user_track SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    int u4 = stmt.executeUpdate("UPDATE tu_title_recommendation SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    int u5 = stmt.executeUpdate("UPDATE tu_daily_recommend SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    int u6 = stmt.executeUpdate("UPDATE tu_reference_post SET track_id = '" + keepId + "' WHERE track_id = '" + oldId + "'");
                    int u7 = stmt.executeUpdate("UPDATE tu_track SET is_deleted = 1 WHERE id = '" + oldId + "'");
                    System.out.println("[合并重复赛道]   合并 " + oldId + " -> " + keepId
                        + " (blogger=" + u1 + ", title=" + u2 + ", userTrack=" + u3 + ", rec=" + u4
                        + ", daily=" + u5 + ", ref=" + u6 + ", del=" + u7 + ")");
                }
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
            track.setIsDeleted(0);
            trackMapper.insert(track);
        } else {
            if (track.getIsDeleted() == null) {
                track.setIsDeleted(0);
            }
            trackMapper.update(track);
        }
    }

    public void delete(String id) {
        trackMapper.delete(id);
    }
}
