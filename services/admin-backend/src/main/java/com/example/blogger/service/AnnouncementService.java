package com.example.blogger.service;

import com.example.blogger.entity.Announcement;
import com.example.blogger.mapper.AnnouncementMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AnnouncementService {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementService.class);

    private final AnnouncementMapper announcementMapper;

    public AnnouncementService(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    public List<Announcement> listAll() {
        return announcementMapper.findAll();
    }

    public Announcement getById(String id) {
        return announcementMapper.findById(id);
    }

    public Announcement getActiveByType(String type) {
        return announcementMapper.findActiveByType(type);
    }

    public void save(Announcement announcement) {
        if (announcement.getId() == null || announcement.getId().isEmpty()) {
            // 新增前校验同类型唯一
            Announcement existing = announcementMapper.findByType(announcement.getType());
            if (existing != null) {
                throw new RuntimeException("该类型的公告已存在，请勿重复添加");
            }
            announcement.setId(UUID.randomUUID().toString().replace("-", ""));
            announcement.setIsEnabled(announcement.getIsEnabled() != null ? announcement.getIsEnabled() : 1);
            announcementMapper.insert(announcement);
            log.info("[Announcement] 新增公告: type={}", announcement.getType());
        } else {
            Announcement old = announcementMapper.findById(announcement.getId());
            if (old == null) {
                throw new RuntimeException("公告不存在");
            }
            // 如果修改了类型，校验新类型是否已存在（且不是自己）
            if (!old.getType().equals(announcement.getType())) {
                Announcement existing = announcementMapper.findByType(announcement.getType());
                if (existing != null && !existing.getId().equals(announcement.getId())) {
                    throw new RuntimeException("该类型的公告已存在，请勿重复添加");
                }
            }
            announcementMapper.update(announcement);
            log.info("[Announcement] 更新公告: id={}", announcement.getId());
        }
    }

    public void delete(String id) {
        announcementMapper.softDelete(id);
        log.info("[Announcement] 删除公告: id={}", id);
    }
}
