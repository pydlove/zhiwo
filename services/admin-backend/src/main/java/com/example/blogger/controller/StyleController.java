package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.Style;
import com.example.blogger.service.StyleService;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/styles")
@CrossOrigin(origins = "*")
public class StyleController {
    private final StyleService styleService;

    public StyleController(StyleService styleService) {
        this.styleService = styleService;
    }

    @GetMapping
    public Result<List<Style>> list() {
        return Result.ok(styleService.list());
    }

    @GetMapping("/{id}")
    public Result<Style> get(@PathVariable String id) {
        return Result.ok(styleService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Style style) {
        styleService.save(style);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody Style style) {
        Style existing = styleService.getById(id);
        if (existing == null) {
            return Result.error("模板不存在");
        }
        if (style.getName() != null) existing.setName(style.getName());
        if (style.getScene() != null) existing.setScene(style.getScene());
        if (style.getIsDefault() != null) existing.setIsDefault(style.getIsDefault());
        if (style.getStatus() != null) existing.setStatus(style.getStatus());
        if (style.getStyleJson() != null) existing.setStyleJson(style.getStyleJson());
        styleService.save(existing);
        if (Integer.valueOf(1).equals(style.getIsDefault())) {
            styleService.setDefault(id);
        }
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        styleService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/export")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> exportStyles(@RequestBody Map<String, Object> params) {
        String targetDir = params != null && params.get("targetDir") != null
                ? params.get("targetDir").toString()
                : System.getProperty("user.dir") + File.separator + "styles";

        List<String> styleIds = new ArrayList<>();
        if (params != null && params.get("styleIds") instanceof List) {
            styleIds = (List<String>) params.get("styleIds");
        }

        File outputDir = new File(targetDir);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Source directory for style docx files
        String sourceDir = "/Users/panyong/aio_project/公众号/样式";
        File sourceDirFile = new File(sourceDir);

        List<String> exportedFiles = new ArrayList<>();
        List<String> missingStyles = new ArrayList<>();

        List<Style> styles;
        if (styleIds != null && !styleIds.isEmpty()) {
            styles = new ArrayList<>();
            for (String id : styleIds) {
                Style s = styleService.getById(id);
                if (s != null) styles.add(s);
            }
        } else {
            styles = styleService.list();
        }

        for (Style style : styles) {
            if (style.getName() == null || style.getName().isEmpty()) continue;

            // Look for matching docx file
            String docxName = style.getName() + ".docx";
            File sourceFile = new File(sourceDirFile, docxName);
            if (!sourceFile.exists()) {
                missingStyles.add(style.getName());
                continue;
            }

            File targetFile = new File(outputDir, docxName);
            try {
                Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                exportedFiles.add(docxName);
            } catch (IOException e) {
                missingStyles.add(style.getName() + " (复制失败: " + e.getMessage() + ")");
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("targetDir", targetDir);
        result.put("exportedCount", exportedFiles.size());
        result.put("exportedFiles", exportedFiles);
        result.put("missingCount", missingStyles.size());
        result.put("missingStyles", missingStyles);
        return Result.ok(result);
    }
}
