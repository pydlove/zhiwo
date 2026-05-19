package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.TitleBannedWord;
import com.example.blogger.mapper.TitleBannedWordMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/title-banned-words")
@CrossOrigin(origins = "*")
public class TitleBannedWordController {

    private final TitleBannedWordMapper titleBannedWordMapper;

    public TitleBannedWordController(TitleBannedWordMapper titleBannedWordMapper) {
        this.titleBannedWordMapper = titleBannedWordMapper;
    }

    @GetMapping
    public Result<List<TitleBannedWord>> list() {
        return Result.ok(titleBannedWordMapper.findAll());
    }

    @GetMapping("/active")
    public Result<List<TitleBannedWord>> listActive() {
        return Result.ok(titleBannedWordMapper.findAllActive());
    }

    @PostMapping
    public Result<TitleBannedWord> save(@RequestBody TitleBannedWord word) {
        if (word.getId() == null || word.getId().isEmpty()) {
            word.setId(UUID.randomUUID().toString().replace("-", ""));
            if (word.getIsActive() == null) word.setIsActive(1);
            if (word.getCategory() == null) word.setCategory("通用");
            titleBannedWordMapper.insert(word);
        } else {
            titleBannedWordMapper.update(word);
        }
        return Result.ok(word);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        titleBannedWordMapper.delete(id);
        return Result.ok(null);
    }
}
