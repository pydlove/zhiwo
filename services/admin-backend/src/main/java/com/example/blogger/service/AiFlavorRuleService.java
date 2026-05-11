package com.example.blogger.service;

import com.example.blogger.entity.AiFlavorRule;
import com.example.blogger.mapper.AiFlavorRuleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AiFlavorRuleService {

    @Autowired
    private AiFlavorRuleMapper mapper;

    public List<AiFlavorRule> list() {
        return mapper.findAll();
    }

    public void save(AiFlavorRule rule) {
        if (rule.getId() == null || rule.getId().isEmpty()) {
            rule.setId(UUID.randomUUID().toString().replace("-", ""));
            if (rule.getSortOrder() == null) rule.setSortOrder(0);
            if (rule.getIsEnabled() == null) rule.setIsEnabled(1);
            mapper.insert(rule);
        } else {
            mapper.update(rule);
        }
    }

    public void delete(String id) {
        mapper.deleteById(id);
    }
}