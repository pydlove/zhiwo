package com.example.blogger.service;

import com.example.blogger.entity.Activity;
import com.example.blogger.mapper.ActivityMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class ActivityService {
    private final ActivityMapper activityMapper;

    public ActivityService(ActivityMapper activityMapper) {
        this.activityMapper = activityMapper;
    }

    public List<Activity> list() {
        return activityMapper.findAll();
    }

    public Activity getById(String id) {
        return activityMapper.findById(id);
    }

    public void save(Activity activity) {
        if (activity.getId() == null || activity.getId().isEmpty()) {
            activity.setId(UUID.randomUUID().toString().replace("-", ""));
            if (activity.getStatus() == null) activity.setStatus(1);
            if (activity.getSortOrder() == null) activity.setSortOrder(0);
            activityMapper.insert(activity);
        } else {
            activityMapper.update(activity);
        }
    }

    public void delete(String id) {
        activityMapper.delete(id);
    }
}
