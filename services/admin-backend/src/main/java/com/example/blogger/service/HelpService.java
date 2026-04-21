package com.example.blogger.service;

import com.example.blogger.entity.Help;
import com.example.blogger.mapper.HelpMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

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

    public void save(Help help) {
        if (help.getId() == null || help.getId().isEmpty()) {
            help.setId(UUID.randomUUID().toString().replace("-", ""));
            helpMapper.insert(help);
        } else {
            helpMapper.update(help);
        }
    }

    public void delete(String id) {
        helpMapper.delete(id);
    }
}
