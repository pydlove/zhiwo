package com.example.user.service;

import com.example.user.entity.Post;
import com.example.user.mapper.PostMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class PostService {
    private final PostMapper postMapper;

    public PostService(PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    public List<Post> listByBlogger(String bloggerId) {
        return postMapper.findByBloggerId(bloggerId);
    }

    public List<Post> listByTrack(String trackId) {
        return postMapper.findByTrackId(trackId);
    }

    public List<Post> search(String trackId, String platform, String keyword) {
        return postMapper.search(trackId, platform, keyword);
    }

    public void save(Post post) {
        if (post.getId() == null || post.getId().isEmpty()) {
            post.setId(UUID.randomUUID().toString().replace("-", ""));
            postMapper.insert(post);
        } else {
            postMapper.update(post);
        }
    }

    public void delete(String id) {
        postMapper.delete(id);
    }

    public java.util.List<java.util.Map<String, Object>> listRecommendations() {
        return postMapper.findRecommendations(6);
    }

    public java.util.List<java.util.Map<String, Object>> listRecommendationsByTrack(String trackId) {
        return postMapper.findRecommendationsByTrack(trackId);
    }
}
