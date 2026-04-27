package com.example.blogger.controller;

import com.example.blogger.entity.Result;
import com.example.blogger.entity.Post;
import com.example.blogger.service.PostService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public Result<List<Post>> list(@RequestParam(required = false) String bloggerId) {
        if (bloggerId != null && !bloggerId.isEmpty()) {
            return Result.ok(postService.listByBlogger(bloggerId));
        }
        return Result.ok(postService.listAll());
    }

    @GetMapping("/search")
    public Result<List<Post>> search(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String trackId,
            @RequestParam(required = false) String keyword) {
        return Result.ok(postService.search(platform, trackId, keyword));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Post post) {
        postService.save(post);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        postService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/export")
    public void export(HttpServletResponse response,
            @RequestParam(value = "postIds", required = false) List<String> postIds) {
        try {
            List<Post> posts;
            if (postIds != null && !postIds.isEmpty()) {
                posts = new ArrayList<>();
                for (String id : postIds) {
                    Post p = postService.search(null, null, null).stream()
                            .filter(x -> id.equals(x.getId()))
                            .findFirst().orElse(null);
                    if (p == null) {
                        p = postService.listAll().stream()
                                .filter(x -> id.equals(x.getId()))
                                .findFirst().orElse(null);
                    }
                    if (p != null) posts.add(p);
                }
            } else {
                posts = postService.listAll();
            }

            XSSFWorkbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet("文章管理");
            String[] headers = { "ID", "文章标题", "原文链接", "博主ID", "平台", "阅读量", "点赞量", "评论量", "指标JSON", "状态", "摘要", "标签" };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            for (int i = 0; i < posts.size(); i++) {
                Post p = posts.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(p.getId() != null ? p.getId() : "");
                row.createCell(1).setCellValue(p.getTitle() != null ? p.getTitle() : "");
                row.createCell(2).setCellValue(p.getUrl() != null ? p.getUrl() : "");
                row.createCell(3).setCellValue(p.getBloggerId() != null ? p.getBloggerId() : "");
                row.createCell(4).setCellValue(p.getPlatform() != null ? p.getPlatform() : "");
                row.createCell(5).setCellValue(p.getReads() != null ? p.getReads() : "");
                row.createCell(6).setCellValue(p.getLikes() != null ? p.getLikes() : "");
                row.createCell(7).setCellValue(p.getComments() != null ? p.getComments() : "");
                row.createCell(8).setCellValue(p.getMetricsJson() != null ? p.getMetricsJson() : "");
                row.createCell(9).setCellValue(p.getStatus() != null ? p.getStatus() : "");
                row.createCell(10).setCellValue(p.getSummary() != null ? p.getSummary() : "");
                row.createCell(11).setCellValue(p.getTag() != null ? p.getTag() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }

            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "文章管理导出_" + timestamp + ".xlsx";

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
                String title = getCellString(row, 1);
                String url = getCellString(row, 2);
                String bloggerId = getCellString(row, 3);
                String platform = getCellString(row, 4);
                String reads = getCellString(row, 5);
                String likes = getCellString(row, 6);
                String comments = getCellString(row, 7);
                String metricsJson = getCellString(row, 8);
                String status = getCellString(row, 9);
                String summary = getCellString(row, 10);
                String tag = getCellString(row, 11);

                if (title == null || title.isEmpty()) {
                    skip++;
                    continue;
                }

                Post post = new Post();
                post.setTitle(title);
                post.setUrl(url);
                post.setBloggerId(bloggerId);
                post.setPlatform(platform);
                post.setReads(reads);
                post.setLikes(likes);
                post.setComments(comments);
                post.setMetricsJson(metricsJson);
                post.setStatus(status != null && !status.isEmpty() ? status : "已上架");
                post.setSummary(summary);
                post.setTag(tag);

                if (id != null && !id.isEmpty()) {
                    List<Post> all = postService.listAll();
                    Post existing = all.stream().filter(p -> id.equals(p.getId())).findFirst().orElse(null);
                    if (existing != null) {
                        post.setId(id);
                        postService.save(post);
                        updated++;
                        success++;
                        continue;
                    }
                }

                postService.save(post);
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
