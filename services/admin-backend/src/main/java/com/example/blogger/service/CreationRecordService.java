package com.example.blogger.service;

import com.example.blogger.entity.CreationRecord;
import com.example.blogger.mapper.CreationRecordMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class CreationRecordService {
    private final CreationRecordMapper creationRecordMapper;

    public CreationRecordService(CreationRecordMapper creationRecordMapper) {
        this.creationRecordMapper = creationRecordMapper;
    }

    public List<CreationRecord> list(String userId, String trackId, String date) {
        return creationRecordMapper.findAll(userId, trackId, date);
    }

    public CreationRecord getById(String id) {
        return creationRecordMapper.findById(id);
    }

    public void save(CreationRecord record) {
        if (record.getId() == null || record.getId().isEmpty()) {
            record.setId(UUID.randomUUID().toString().replace("-", ""));
            if (record.getReviewed() == null) record.setReviewed(0);
            creationRecordMapper.insert(record);
        } else {
            creationRecordMapper.update(record);
        }
    }

    public void markReviewed(String id) {
        creationRecordMapper.markReviewed(id);
    }

    public void delete(String id) {
        creationRecordMapper.delete(id);
    }
}
