package com.example.user.service;

import com.example.user.entity.Help;
import com.example.user.mapper.HelpMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HelpService {
    private final HelpMapper helpMapper;

    public HelpService(HelpMapper helpMapper) {
        this.helpMapper = helpMapper;
    }

    public List<Help> list() {
        return helpMapper.findAll();
    }

    public Help getById(String id) {
        return helpMapper.findById(id);
    }
}
