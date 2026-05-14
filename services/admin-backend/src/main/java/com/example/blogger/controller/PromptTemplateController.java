package com.example.blogger.controller;

import com.example.blogger.entity.PromptTemplate;
import com.example.blogger.entity.Result;
import com.example.blogger.mapper.PromptTemplateMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/prompt-templates")
@CrossOrigin(origins = "*")
public class PromptTemplateController {

    private final PromptTemplateMapper promptTemplateMapper;

    public PromptTemplateController(PromptTemplateMapper promptTemplateMapper) {
        this.promptTemplateMapper = promptTemplateMapper;
    }

    @GetMapping
    public Result<List<PromptTemplate>> list(@RequestParam(value = "type", required = false) String type) {
        if (type != null && !type.isEmpty()) {
            // Filter by type in Java since we fetch all
            List<PromptTemplate> all = promptTemplateMapper.findAll();
            List<PromptTemplate> filtered = new ArrayList<>();
            for (PromptTemplate pt : all) {
                if (type.equals(pt.getType())) {
                    filtered.add(pt);
                }
            }
            return Result.ok(filtered);
        }
        return Result.ok(promptTemplateMapper.findAll());
    }

    @GetMapping("/default")
    public Result<PromptTemplate> getDefault(@RequestParam("type") String type) {
        PromptTemplate pt = promptTemplateMapper.findDefaultByType(type);
        if (pt == null) {
            pt = promptTemplateMapper.findLatestByType(type);
        }
        return Result.ok(pt);
    }

    @PostMapping
    public Result<PromptTemplate> save(@RequestBody PromptTemplate template) {
        if (template.getId() == null || template.getId().isEmpty()) {
            template.setId(UUID.randomUUID().toString().replace("-", ""));
            if (template.getType() == null || template.getType().isEmpty()) {
                template.setType("generate_title");
            }
            if (template.getIsDefault() == null) {
                template.setIsDefault(0);
            }
            if (Integer.valueOf(1).equals(template.getIsDefault())) {
                promptTemplateMapper.clearDefaultByType(template.getType());
            }
            promptTemplateMapper.insert(template);
        } else {
            PromptTemplate existing = promptTemplateMapper.findById(template.getId());
            if (existing == null) {
                return Result.error("模板不存在");
            }
            if (template.getName() != null) existing.setName(template.getName());
            if (template.getContent() != null) existing.setContent(template.getContent());
            if (template.getType() != null) existing.setType(template.getType());
            if (template.getIsDefault() != null) existing.setIsDefault(template.getIsDefault());
            if (Integer.valueOf(1).equals(existing.getIsDefault())) {
                promptTemplateMapper.clearDefaultByType(existing.getType());
            }
            promptTemplateMapper.update(existing);
            template = existing;
        }
        return Result.ok(template);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        promptTemplateMapper.delete(id);
        return Result.ok(null);
    }
}
