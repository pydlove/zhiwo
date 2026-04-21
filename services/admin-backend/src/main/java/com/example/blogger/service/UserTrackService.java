package com.example.blogger.service;

import com.example.blogger.entity.UserTrack;
import com.example.blogger.mapper.UserTrackMapper;
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

    public void save(UserTrack userTrack) {
        userTrackMapper.insert(userTrack);
    }

    public void delete(String userId, String trackId) {
        userTrackMapper.delete(userId, trackId);
    }

    public int countByUser(String userId) {
        return userTrackMapper.countByUserId(userId);
    }
}
