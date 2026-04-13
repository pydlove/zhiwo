package com.example.blogger.service;

import com.example.blogger.entity.Post;
import com.example.blogger.mapper.PostMapper;
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
}
