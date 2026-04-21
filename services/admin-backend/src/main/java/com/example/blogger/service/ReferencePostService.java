package com.example.blogger.service;

import com.example.blogger.entity.ReferencePost;
import com.example.blogger.mapper.ReferencePostMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class ReferencePostService {
    private final ReferencePostMapper referencePostMapper;

    public ReferencePostService(ReferencePostMapper referencePostMapper) {
        this.referencePostMapper = referencePostMapper;
    }

    public List<ReferencePost> list() {
        return referencePostMapper.findAll();
    }

    public ReferencePost getById(String id) {
        return referencePostMapper.findById(id);
    }

    public void save(ReferencePost r) {
        if (r.getId() == null || r.getId().isEmpty()) {
            r.setId(UUID.randomUUID().toString().replace("-", ""));
            if (r.getStatus() == null || r.getStatus().isEmpty()) {
                r.setStatus("已上架");
            }
            referencePostMapper.insert(r);
        } else {
            referencePostMapper.update(r);
        }
    }

    public void delete(String id) {
        referencePostMapper.delete(id);
    }
}
