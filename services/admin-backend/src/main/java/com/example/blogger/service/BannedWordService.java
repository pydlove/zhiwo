package com.example.blogger.service;

import com.example.blogger.entity.BannedWord;
import com.example.blogger.mapper.BannedWordMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BannedWordService {

    private final BannedWordMapper bannedWordMapper;

    public BannedWordService(BannedWordMapper bannedWordMapper) {
        this.bannedWordMapper = bannedWordMapper;
    }

    public List<BannedWord> list() {
        return bannedWordMapper.findAll();
    }

    public BannedWord getById(String id) {
        return bannedWordMapper.findById(id);
    }

    public void save(BannedWord bannedWord) {
        if (bannedWord.getId() == null || bannedWord.getId().isEmpty()) {
            bannedWord.setId(UUID.randomUUID().toString().replace("-", ""));
            bannedWordMapper.insert(bannedWord);
        } else {
            bannedWordMapper.update(bannedWord);
        }
    }

    public void delete(String id) {
        bannedWordMapper.delete(id);
    }
}
