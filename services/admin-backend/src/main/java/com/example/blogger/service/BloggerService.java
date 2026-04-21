package com.example.blogger.service;

import com.example.blogger.entity.Blogger;
import com.example.blogger.mapper.BloggerMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class BloggerService {
    private final BloggerMapper bloggerMapper;

    public BloggerService(BloggerMapper bloggerMapper) {
        this.bloggerMapper = bloggerMapper;
    }

    public List<Blogger> listByTrack(String trackId) {
        return bloggerMapper.findByTrackId(trackId);
    }

    public List<Blogger> listAll() {
        return bloggerMapper.findAll();
    }

    public Blogger getById(String id) {
        return bloggerMapper.findById(id);
    }

    public void save(Blogger blogger) {
        if (blogger.getId() == null || blogger.getId().isEmpty()) {
            blogger.setId(UUID.randomUUID().toString().replace("-", ""));
            bloggerMapper.insert(blogger);
        } else {
            bloggerMapper.update(blogger);
        }
    }

    public void delete(String id) {
        bloggerMapper.delete(id);
    }
}
