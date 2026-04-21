package com.example.user.service;

import com.example.user.entity.UserTrack;
import com.example.user.mapper.UserTrackMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserTrackService {
    private final UserTrackMapper userTrackMapper;

    public UserTrackService(UserTrackMapper userTrackMapper) {
        this.userTrackMapper = userTrackMapper;
    }

    public List<UserTrack> listByUser(String userId) {
        return userTrackMapper.findByUserId(userId);
    }

    public int countByUser(String userId) {
        return userTrackMapper.countByUserId(userId);
    }

    public void save(UserTrack userTrack) {
        userTrackMapper.insert(userTrack);
    }

    public void delete(String userId, String trackId) {
        userTrackMapper.delete(userId, trackId);
    }
}
