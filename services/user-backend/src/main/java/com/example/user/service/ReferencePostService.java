package com.example.user.service;

import com.example.user.entity.ReferencePost;
import com.example.user.mapper.ReferencePostMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ReferencePostService {
    private final ReferencePostMapper referencePostMapper;

    public ReferencePostService(ReferencePostMapper referencePostMapper) {
        this.referencePostMapper = referencePostMapper;
    }

    public List<ReferencePost> listByTrackAndPlatform(String trackId, String platform) {
        return referencePostMapper.findByTrackAndPlatform(trackId, platform);
    }
}
