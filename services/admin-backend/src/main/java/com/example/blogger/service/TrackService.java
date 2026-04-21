package com.example.blogger.service;

import com.example.blogger.entity.Track;
import com.example.blogger.mapper.TrackMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class TrackService {
    private final TrackMapper trackMapper;

    public TrackService(TrackMapper trackMapper) {
        this.trackMapper = trackMapper;
    }

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
