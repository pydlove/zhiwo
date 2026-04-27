package com.example.blogger.controller;

import com.example.blogger.entity.ReferencePost;
import com.example.blogger.entity.Result;
import com.example.blogger.service.ReferencePostService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/reference-posts")
@CrossOrigin(origins = "*")
public class ReferencePostController {
    private final ReferencePostService referencePostService;

    public ReferencePostController(ReferencePostService referencePostService) {
        this.referencePostService = referencePostService;
    }

    @GetMapping
    public Result<List<ReferencePost>> list() {
        return Result.ok(referencePostService.list());
    }

    @GetMapping("/{id}")
    public Result<ReferencePost> get(@PathVariable String id) {
        return Result.ok(referencePostService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody ReferencePost r) {
        referencePostService.save(r);
        return Result.ok(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody ReferencePost r) {
        r.setId(id);
        referencePostService.save(r);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        referencePostService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/export")
    public void export(HttpServletResponse response,
            @RequestParam(value = "refIds", required = false) List<String> refIds) {
        try {
            List<ReferencePost> list = referencePostService.list();
            List<ReferencePost> posts;
            if (refIds != null && !refIds.isEmpty()) {
                posts = new ArrayList<>();
                for (String id : refIds) {
                    ReferencePost p = list.stream()
                            .filter(x -> id.equals(x.getId()))
                            .findFirst().orElse(null);
                    if (p != null) posts.add(p);
                }
            } else {
                posts = list;
            }

            XSSFWorkbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet("帮助管理");
            String[] headers = { "ID", "所属赛道ID", "平台", "文章标题", "原文内容", "外部链接", "排序", "状态" };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            for (int i = 0; i < posts.size(); i++) {
                ReferencePost p = posts.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(p.getId() != null ? p.getId() : "");
                row.createCell(1).setCellValue(p.getTrackId() != null ? p.getTrackId() : "");
                row.createCell(2).setCellValue(p.getPlatform() != null ? p.getPlatform() : "");
                row.createCell(3).setCellValue(p.getTitle() != null ? p.getTitle() : "");
                row.createCell(4).setCellValue(p.getContent() != null ? p.getContent() : "");
                row.createCell(5).setCellValue(p.getUrl() != null ? p.getUrl() : "");
                row.createCell(6).setCellValue(p.getSortOrder() != null ? p.getSortOrder() : 0);
                row.createCell(7).setCellValue(p.getStatus() != null ? p.getStatus() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }

            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "帮助管理导出_" + timestamp + ".xlsx";

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
            try (OutputStream out = response.getOutputStream()) {
                wb.write(out);
            }
            wb.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "导出失败：" + e.getMessage());
            } catch (IOException ignored) {}
        }
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importExcel(@RequestParam("excel") MultipartFile excel) {
        try {
            Workbook wb = new XSSFWorkbook(excel.getInputStream());
            Sheet sheet = wb.getSheetAt(0);
            List<String> errors = new ArrayList<>();
            int success = 0;
            int created = 0;
            int updated = 0;
            int skip = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String id = getCellString(row, 0);
                String trackId = getCellString(row, 1);
                String platform = getCellString(row, 2);
                String title = getCellString(row, 3);
                String content = getCellString(row, 4);
                String url = getCellString(row, 5);
                String sortOrderStr = getCellString(row, 6);
                String status = getCellString(row, 7);

                if (title == null || title.isEmpty()) {
                    skip++;
                    continue;
                }

                int sortOrder = 0;
                try {
                    if (sortOrderStr != null && !sortOrderStr.isEmpty()) {
                        sortOrder = (int) Double.parseDouble(sortOrderStr);
                    }
                } catch (Exception ignored) {}

                ReferencePost post = new ReferencePost();
                post.setTrackId(trackId);
                post.setPlatform(platform);
                post.setTitle(title);
                post.setContent(content);
                post.setUrl(url);
                post.setSortOrder(sortOrder);
                post.setStatus(status != null && !status.isEmpty() ? status : "已上架");

                if (id != null && !id.isEmpty()) {
                    ReferencePost existing = referencePostService.getById(id);
                    if (existing != null) {
                        post.setId(id);
                        referencePostService.save(post);
                        updated++;
                        success++;
                        continue;
                    }
                }

                referencePostService.save(post);
                created++;
                success++;
            }
            wb.close();

            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("created", created);
            result.put("updated", updated);
            result.put("skip", skip);
            result.put("errors", errors);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) {
            String v = cell.getStringCellValue();
            return v != null ? v.trim() : null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return null;
    }
}
