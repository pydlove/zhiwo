package com.example.user.service;

import com.example.user.entity.Style;
import com.example.user.mapper.StyleMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class StyleService {
    private final StyleMapper styleMapper;

    public StyleService(StyleMapper styleMapper) {
        this.styleMapper = styleMapper;
    }

    public List<Style> listEnabled() {
        return styleMapper.findAllEnabled();
    }

    public Style getById(String id) {
        return styleMapper.findById(id);
    }
}
