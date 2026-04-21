package com.example.user.service;

import com.example.user.entity.HelpCategory;
import com.example.user.mapper.HelpCategoryMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HelpCategoryService {
    private final HelpCategoryMapper helpCategoryMapper;

    public HelpCategoryService(HelpCategoryMapper helpCategoryMapper) {
        this.helpCategoryMapper = helpCategoryMapper;
    }

    public List<HelpCategory> list() {
        return helpCategoryMapper.findAll();
    }
}
