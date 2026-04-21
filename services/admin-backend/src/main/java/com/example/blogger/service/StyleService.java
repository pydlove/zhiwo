package com.example.blogger.service;

import com.example.blogger.entity.Style;
import com.example.blogger.mapper.StyleMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class StyleService {
    private final StyleMapper styleMapper;

    public StyleService(StyleMapper styleMapper) {
        this.styleMapper = styleMapper;
    }

    public List<Style> list() {
        return styleMapper.findAll();
    }

    public Style getById(String id) {
        return styleMapper.findById(id);
    }

    public void save(Style style) {
        if (style.getId() == null || style.getId().isEmpty()) {
            style.setId(UUID.randomUUID().toString().replace("-", ""));
            if (style.getIsDefault() == null) style.setIsDefault(0);
            if (style.getStatus() == null) style.setStatus("已启用");
            styleMapper.insert(style);
        } else {
            styleMapper.update(style);
        }
    }

    public void setDefault(String id) {
        styleMapper.clearDefault();
        Style style = styleMapper.findById(id);
        if (style != null) {
            style.setIsDefault(1);
            styleMapper.update(style);
        }
    }

    public void delete(String id) {
        styleMapper.delete(id);
    }
}
