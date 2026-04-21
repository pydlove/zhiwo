package com.example.blogger.service;

import com.example.blogger.entity.HelpCategory;
import com.example.blogger.mapper.HelpCategoryMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class HelpCategoryService {
    private final HelpCategoryMapper helpCategoryMapper;

    public HelpCategoryService(HelpCategoryMapper helpCategoryMapper) {
        this.helpCategoryMapper = helpCategoryMapper;
    }

    public List<HelpCategory> list() {
        return helpCategoryMapper.findAll();
    }

    public HelpCategory getById(String id) {
        return helpCategoryMapper.findById(id);
    }

    public void save(HelpCategory category) {
        if (category.getId() == null || category.getId().isEmpty()) {
            category.setId(UUID.randomUUID().toString().replace("-", ""));
            helpCategoryMapper.insert(category);
        } else {
            helpCategoryMapper.update(category);
        }
    }

    public void delete(String id) {
        helpCategoryMapper.delete(id);
    }
}
